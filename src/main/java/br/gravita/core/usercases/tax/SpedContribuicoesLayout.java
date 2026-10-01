package br.gravita.core.usercases.tax;

import static br.gravita.core.usercases.tax.SpedContribuicoesDocuments.money;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Incidence;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedBlock;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedLayout;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedRecord;
import br.gravita.core.usercases.tax.SpedContribuicoesAssessor.Credited;
import br.gravita.core.usercases.tax.SpedContribuicoesAssessor.Levied;
import br.gravita.core.usercases.tax.SpedContribuicoesAssessor.Result;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Item;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Levy;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Operation;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Party;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Lays an EFD Contribuições out as registers, in the order and with the fields of the RFB's "Guia Prático". The
 * company's documents go in Block C ({@code C100} with a {@code C170} per item, "escrituração detalhada"), the
 * assessment in Block M; Blocks A, D, F, I, P and 1 (services, transport, other operations, CPRB and the rest) carry
 * nothing, as the documents of this module are all NFe of goods. Block 9 and the {@code X001}/{@code X990} pairs are
 * the file writer's.
 *
 * <p>Fields the module holds no data for are left empty rather than made up: the company's legal name and
 * municipality code (the {@code Company} aggregate carries neither), the participants' address and municipality, and
 * the ICMS and IPI of the items (a PIS/COFINS file does not need them).
 */
final class SpedContribuicoesLayout {

	/** {@code COD_VER}: the layout version of the Guia Prático this file follows. */
	static final String LAYOUT_VERSION = "006";

	private static final String BRAZIL = "01058";
	private static final String NATURE_GENERAL = "00";
	private static final String ORIGINAL_FILE = "0";
	private static final String NFE_MODEL = "55";
	private static final String REGULAR = "00";
	private static final String NO_PAYMENT_OR_FREIGHT = "9";
	private static final String DETAILED_BOOKKEEPING = "2";
	private static final String CREDIT_CODE_BASIC_RATE_TAXED_SALES = "101";
	private static final String OWN_OPERATIONS = "0";
	private static final String CREDITED_CST = "50";

	private SpedContribuicoesLayout() {
	}

	static SpedLayout build(Company company, YearMonth period, Incidence incidence,
			List<SpedContribuicoesDocument> documents, Result pis, Result cofins) {
		String cnpj = company.getCnpj().number();
		SpedRecord header = SpedRecord.of("0000", LAYOUT_VERSION, ORIGINAL_FILE, null, null, period.atDay(1),
				period.atEndOfMonth(), null, cnpj, company.getState(), null, null, NATURE_GENERAL,
				activity(company.getCnae()));
		return new SpedLayout(header,
				List.of(new SpedBlock('0', blockZero(company, cnpj, incidence, documents, pis, cofins)),
						new SpedBlock('A', List.of()), new SpedBlock('C', blockC(cnpj, documents)),
						new SpedBlock('D', List.of()), new SpedBlock('F', List.of()), new SpedBlock('I', List.of()),
						new SpedBlock('M', blockM(incidence, documents, pis, cofins)), new SpedBlock('P', List.of()),
						new SpedBlock('1', List.of())));
	}

	private static List<SpedRecord> blockZero(Company company, String cnpj, Incidence incidence,
			List<SpedContribuicoesDocument> documents, Result pis, Result cofins) {
		List<SpedRecord> records = new ArrayList<>();
		boolean nonCumulative = incidence == Incidence.NON_CUMULATIVE;
		boolean general = pis.onlyGeneralRate(incidence) && cofins.onlyGeneralRate(incidence);
		// COD_INC_TRIB (1 non-cumulative, 2 cumulative), IND_APRO_CRED (1 direct appropriation), COD_TIPO_CONT (1 general
		// rate only, 2 other rates too) and IND_REG_CUM (9 accrual basis, itemized by the documents of Blocks A, C, D, F).
		records.add(SpedRecord.of("0110", nonCumulative ? "1" : "2", nonCumulative ? "1" : null,
				nonCumulative ? (general ? "1" : "2") : null, nonCumulative ? null : "9"));
		String ie = company.getIe().chars().allMatch(Character::isDigit) ? company.getIe() : null;
		records.add(SpedRecord.of("0140", cnpj, null, cnpj, company.getState(), ie, null, company.getIm(), null));

		Map<String, Party> parties = new TreeMap<>();
		Map<String, String> units = new TreeMap<>();
		Map<String, Item> items = new TreeMap<>();
		for (SpedContribuicoesDocument document : documents) {
			parties.putIfAbsent(document.party().code(), document.party());
			for (Item item : document.items()) {
				units.putIfAbsent(item.unit(), item.unit());
				items.putIfAbsent(item.code(), item);
			}
		}
		parties.values().forEach(party -> records.add(SpedRecord.of("0150", party.code(), party.name(), BRAZIL,
				party.cnpj(), party.cpf(), null, null, null, null, null, null)));
		new TreeSet<>(units.values()).forEach(unit -> records.add(SpedRecord.of("0190", unit, unit)));
		// TIPO_ITEM 99: the module does not classify what it sells or buys.
		items.values().forEach(item -> records.add(SpedRecord.of("0200", item.code(), item.description(), null, null,
				item.unit(), "99", item.ncm(), null, null, null, null)));
		return records;
	}

	private static List<SpedRecord> blockC(String cnpj, List<SpedContribuicoesDocument> documents) {
		if (documents.isEmpty()) {
			return List.of();
		}
		List<SpedRecord> records = new ArrayList<>();
		records.add(SpedRecord.of("C010", cnpj, DETAILED_BOOKKEEPING));
		for (SpedContribuicoesDocument document : documents) {
			records.add(documentRecord(document));
			int number = 0;
			for (Item item : document.items()) {
				records.add(itemRecord(++number, item));
			}
		}
		return records;
	}

	private static SpedRecord documentRecord(SpedContribuicoesDocument document) {
		boolean exit = document.operation() == Operation.EXIT;
		List<Object> fields = new ArrayList<>();
		fields.add(exit ? "1" : "0");
		fields.add(document.issuedByCompany() ? "0" : "1");
		fields.add(document.party().code());
		fields.add(NFE_MODEL);
		fields.add(REGULAR);
		fields.add(document.series());
		fields.add(document.number());
		fields.add(document.accessKey());
		fields.add(document.date());
		fields.add(document.date());
		fields.add(document.total());
		fields.add(NO_PAYMENT_OR_FREIGHT);
		fields.add(document.discount());
		fields.add(money(BigDecimal.ZERO));
		fields.add(document.goods());
		fields.add(NO_PAYMENT_OR_FREIGHT);
		fields.add(document.freight());
		fields.add(document.insurance());
		fields.add(document.otherExpenses());
		// VL_BC_ICMS, VL_ICMS, VL_BC_ICMS_ST, VL_ICMS_ST, VL_IPI
		nulls(fields, 5);
		fields.add(document.pis());
		fields.add(document.cofins());
		// VL_PIS_ST, VL_COFINS_ST
		nulls(fields, 2);
		return new SpedRecord("C100", fields);
	}

	private static SpedRecord itemRecord(int number, Item item) {
		List<Object> fields = new ArrayList<>();
		fields.add(number);
		fields.add(item.code());
		fields.add(item.description());
		fields.add(item.quantity().setScale(5, RoundingMode.HALF_UP));
		fields.add(item.unit());
		fields.add(item.value());
		fields.add(item.discount());
		// IND_MOV: the goods moved
		fields.add("0");
		// CST_ICMS
		nulls(fields, 1);
		fields.add(item.cfop() == null ? null : Document.digitsOnly(item.cfop()));
		// COD_NAT, VL_BC_ICMS, ALIQ_ICMS, VL_ICMS, VL_BC_ICMS_ST, ALIQ_ST, VL_ICMS_ST, IND_APUR, CST_IPI, COD_ENQ,
		// VL_BC_IPI, ALIQ_IPI, VL_IPI
		nulls(fields, 13);
		addLevy(fields, item.pis());
		addLevy(fields, item.cofins());
		// COD_CTA
		nulls(fields, 1);
		return new SpedRecord("C170", fields);
	}

	/** CST, base, rate, base by quantity, rate by quantity, amount: the last three columns of an item for one tax. */
	private static void addLevy(List<Object> fields, Levy levy) {
		fields.add(levy.cst());
		fields.add(levy.base());
		fields.add(levy.rate());
		nulls(fields, 2);
		fields.add(levy.amount());
	}

	private static List<SpedRecord> blockM(Incidence incidence, List<SpedContribuicoesDocument> documents, Result pis,
			Result cofins) {
		if (documents.isEmpty()) {
			return List.of();
		}
		List<SpedRecord> records = new ArrayList<>();
		for (Result result : List.of(pis, cofins)) {
			SpedTax tax = result.tax();
			if (result.credit().signum() > 0) {
				records.add(creditRecord(tax, result));
				result.credited().forEach(credited -> records.add(creditBaseRecord(tax, credited)));
			}
			records.add(totalRecord(tax, incidence, result));
			result.levied().forEach(levied -> records.add(detailRecord(tax, levied)));
		}
		return records;
	}

	/** {@code M100}/{@code M500}: the credit earned, in full available this period and used up to the contribution. */
	private static SpedRecord creditRecord(SpedTax tax, Result result) {
		boolean usedInFull = result.creditUsed().compareTo(result.credit()) == 0;
		return SpedRecord.of(tax.creditRegister(), CREDIT_CODE_BASIC_RATE_TAXED_SALES, OWN_OPERATIONS,
				result.creditBase(), result.creditRate(), null, null, result.credit(), zero(), zero(), zero(),
				result.credit(), usedInFull ? "0" : "1", result.creditUsed(), result.creditBalance());
	}

	/** {@code M105}/{@code M505}: the base of that credit by nature, all of it linked to non-cumulative taxed sales. */
	private static SpedRecord creditBaseRecord(SpedTax tax, Credited credited) {
		return SpedRecord.of(tax.creditBaseRegister(), credited.nature(), CREDITED_CST, credited.base(), zero(),
				credited.base(), credited.base(), null, null, null);
	}

	/** {@code M200}/{@code M600}: the contribution of the period, non-cumulative part then cumulative part. */
	private static SpedRecord totalRecord(SpedTax tax, Incidence incidence, Result result) {
		boolean nonCumulative = incidence == Incidence.NON_CUMULATIVE;
		BigDecimal due = result.payable();
		return SpedRecord.of(tax.totalRegister(),
				nonCumulative ? result.contribution() : zero(), nonCumulative ? result.creditUsed() : zero(), zero(),
				nonCumulative ? due : zero(), zero(), zero(), nonCumulative ? due : zero(),
				nonCumulative ? zero() : result.contribution(), zero(), zero(), nonCumulative ? zero() : due, due);
	}

	/** {@code M210}/{@code M610}: the contribution levied on one kind of sale. */
	private static SpedRecord detailRecord(SpedTax tax, Levied levied) {
		return SpedRecord.of(tax.detailRegister(), levied.code(), levied.revenue(), levied.base(), levied.rate(), null,
				null, levied.contribution(), zero(), zero(), zero(), zero(), levied.contribution());
	}

	/**
	 * {@code IND_ATIV}: 0 industrial, 2 trade, 9 other - from the division of the company's CNAE, the only sign of
	 * what it does that {@code Company} holds.
	 */
	private static String activity(String cnae) {
		String digits = cnae == null ? "" : Document.digitsOnly(cnae);
		if (digits.length() < 2) {
			return "9";
		}
		int division = Integer.parseInt(digits.substring(0, 2));
		if (division >= 10 && division <= 33) {
			return "0";
		}
		return division >= 45 && division <= 47 ? "2" : "9";
	}

	private static BigDecimal zero() {
		return money(BigDecimal.ZERO);
	}

	private static void nulls(List<Object> fields, int count) {
		for (int i = 0; i < count; i++) {
			fields.add(null);
		}
	}
}
