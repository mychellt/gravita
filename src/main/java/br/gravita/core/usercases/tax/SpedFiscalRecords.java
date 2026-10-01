package br.gravita.core.usercases.tax;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransportModality;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Accountant;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Period;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Taxpayer;
import br.gravita.core.ports.inbound.tax.SpedValidationReport;
import br.gravita.core.ports.inbound.tax.SpedValidationReport.Issue;
import br.gravita.core.ports.inbound.tax.SpedValidationReport.Severity;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedBlock;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort.SpedRecord;
import br.gravita.core.ports.outbound.tax.SpedValues;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The records of one company's EFD ICMS/IPI period, populated from its documents and checked as they are populated
 * (UC-M2-11). It is built per request: documents are added, then {@link #report()} says whether every mandatory
 * record could be filled and {@link #blocks()} gives what the file holds.
 *
 * <p>What the file holds: the identification records (0000, 0005, 0100), the participants (0150), one C100 per
 * document - regular, cancelled (only the identification fields the layout keeps) or a voided number - and the ICMS
 * assessment (E100, E110) of the regular documents; the blocks with nothing to report go out empty. Registers whose
 * data {@code tax} does not hold - the items (C170/0200: unit, NCM per item), the analytic C190 (CST, ICMS rate) -
 * are not generated and are said so in the report's warnings.
 */
final class SpedFiscalRecords {

	/** Layout version of the EFD ICMS/IPI the registers below follow ({@code COD_VER}). */
	static final String LAYOUT_VERSION = "020";

	private static final String COUNTRY_BRAZIL = "01058";
	private static final String MODEL_NFE = "55";
	private static final String REGULAR = "00";
	private static final String CANCELLED = "02";
	private static final String VOIDED_NUMBER = "05";
	private static final String ENTRY = "0";
	private static final String EXIT = "1";
	private static final String OWN_ISSUE = "0";
	private static final String THIRD_PARTY_ISSUE = "1";
	private static final long MAX_DOCUMENT_NUMBER = 999_999_999L;
	private static final Pattern ACCESS_KEY = Pattern.compile("\\d{44}");
	private static final Pattern DIGITS = Pattern.compile("\\d+");
	private static final Pattern MUNICIPALITY_CODE = Pattern.compile("\\d{7}");
	private static final Pattern ZIP_CODE = Pattern.compile("\\d{8}");
	private static final Comparator<Row> ROW_ORDER = Comparator.comparing(Row::date).thenComparing(Row::operation)
			.thenComparing(Row::series).thenComparingLong(Row::number);

	private final GenerateSpedFiscalCommand command;
	private final Company company;
	private final ZoneId zone;
	private final List<Issue> issues = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private final Map<String, SpedRecord> participants = new LinkedHashMap<>();
	private final Set<String> seen = new HashSet<>();
	private BigDecimal icmsDebit = BigDecimal.ZERO;
	private BigDecimal icmsCredit = BigDecimal.ZERO;
	private int regularDocuments;
	private int receivedWithoutIcmsBase;

	SpedFiscalRecords(GenerateSpedFiscalCommand command, Company company, ZoneId zone) {
		this.command = command;
		this.company = company;
		this.zone = zone;
		validateIdentification();
	}

	void addIssued(List<NfeDocument> documents) {
		documents.forEach(this::addIssued);
	}

	void addReceived(List<InboundNfe> documents) {
		documents.forEach(this::addReceived);
	}

	void addVoided(List<VoidedNumberRange> ranges) {
		ranges.forEach(this::addVoided);
	}

	// ---- own NFe -----------------------------------------------------------------------------------------------

	private void addIssued(NfeDocument nfe) {
		String reference = "NFe " + nfe.getDocumentSeries() + "/" + nfe.getDocumentNumber();
		boolean valid = requireKey("C100", reference, nfe.getAccessKey());
		valid &= requireSeries("C100", reference, nfe.getDocumentSeries());
		valid &= requireNumber("C100", reference, nfe.getDocumentNumber());
		if (nfe.getAuthorizedAt() == null) {
			error("C100", reference, "DT_DOC: the document has no authorization date");
			valid = false;
		}
		if (!valid) {
			return;
		}
		if (!requireUnique("C100", reference, "own:" + nfe.getDocumentSeries() + "/" + nfe.getDocumentNumber())
				|| !requireUnique("C100", reference, "key:" + nfe.getAccessKey())) {
			return;
		}
		LocalDate date = nfe.getAuthorizedAt().atZone(zone).toLocalDate();
		String operation = switch (nfe.getCfop().code().charAt(0)) {
			case '1', '2', '3' -> ENTRY;
			default -> EXIT;
		};
		long number = nfe.getDocumentNumber();
		if (nfe.getStatus() == NfeDocumentStatus.CANCELLED) {
			rows.add(new Row(date, operation, nfe.getDocumentSeries(), number, identification(operation, OWN_ISSUE,
					null, CANCELLED, nfe.getDocumentSeries(), number, nfe.getAccessKey())));
			return;
		}
		Document recipient = nfe.getRecipient().document();
		participate(recipient, nfe.getRecipient().name(), nfe.getRecipient().stateRegistration());
		BigDecimal discount = nfe.getItems().stream().map(NfeItem::discount).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		BigDecimal icms = taxTotal(nfe, TaxType.ICMS);
		Amounts amounts = new Amounts(nfe.getDocumentTotal(), discount, nfe.getItemsSubtotal().add(discount),
				freightIndicator(nfe), nfe.getFreight(), nfe.getInsurance(), nfe.getOtherExpenses(),
				icmsBase(nfe), icms, taxTotal(nfe, TaxType.IPI), taxTotal(nfe, TaxType.PIS),
				taxTotal(nfe, TaxType.COFINS));
		rows.add(new Row(date, operation, nfe.getDocumentSeries(), number, regular(operation, OWN_ISSUE,
				recipient.number(), nfe.getDocumentSeries(), number, nfe.getAccessKey(), date, date, amounts)));
		book(operation, icms);
	}

	private static String freightIndicator(NfeDocument nfe) {
		if (nfe.getTransport() == null || nfe.getTransport().modality() == null) {
			return "9";
		}
		return nfe.getTransport().modality() == TransportModality.CIF ? "0" : "1";
	}

	private static BigDecimal icmsBase(NfeDocument nfe) {
		return nfe.getItems().stream().flatMap(item -> item.taxBreakdown().taxLines().stream())
				.filter(line -> line.taxType() == TaxType.ICMS).map(TaxLineBreakdown::base)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal taxTotal(NfeDocument nfe, TaxType type) {
		return nfe.getTaxTotals().byTaxType().getOrDefault(type, BigDecimal.ZERO);
	}

	// ---- received NFe ------------------------------------------------------------------------------------------

	private void addReceived(InboundNfe nfe) {
		String reference = "NFe recebida " + nfe.getSeries() + "/" + nfe.getNumber();
		boolean valid = requireKey("C100", reference, nfe.getAccessKey());
		valid &= requireSeries("C100", reference, nfe.getSeries());
		Long number = parseNumber(nfe.getNumber());
		if (number == null) {
			error("C100", reference, "NUM_DOC: '" + nfe.getNumber() + "' is not a number of up to 9 digits");
			valid = false;
		}
		if (!valid || !requireUnique("C100", reference, "key:" + nfe.getAccessKey())) {
			return;
		}
		LocalDate issued = nfe.getIssuedAt().atZone(zone).toLocalDate();
		LocalDate received = clamp(nfe.getImportedAt().atZone(zone).toLocalDate(), issued, command.period().end());
		participate(nfe.getSupplierDocument(), nfe.getSupplierName(), null);
		var totals = nfe.getTotals();
		Amounts amounts = new Amounts(totals.totalValue(), totals.discountValue(), totals.productsValue(), "9",
				totals.freightValue(), totals.insuranceValue(), totals.otherExpensesValue(), null,
				totals.icmsValue(), totals.ipiValue(), totals.pisValue(), totals.cofinsValue());
		rows.add(new Row(issued, ENTRY, nfe.getSeries(), number, regular(ENTRY, THIRD_PARTY_ISSUE,
				nfe.getSupplierDocument().number(), nfe.getSeries(), number, nfe.getAccessKey(), issued, received,
				amounts)));
		if (totals.icmsValue().signum() > 0) {
			receivedWithoutIcmsBase++;
		}
		book(ENTRY, totals.icmsValue());
	}

	/** The day goods came in cannot precede their issue nor fall after the period the file covers. */
	private static LocalDate clamp(LocalDate day, LocalDate earliest, LocalDate latest) {
		LocalDate bounded = day.isAfter(latest) ? latest : day;
		return bounded.isBefore(earliest) ? earliest : bounded;
	}

	// ---- voided numbers ----------------------------------------------------------------------------------------

	private void addVoided(VoidedNumberRange range) {
		String reference = "inutilização " + range.getSeries() + "/" + range.getStartNumber() + "-"
				+ range.getEndNumber();
		if (range.getEndNumber() > MAX_DOCUMENT_NUMBER) {
			error("C100", reference, "NUM_DOC: numbers above " + MAX_DOCUMENT_NUMBER + " do not fit the layout");
			return;
		}
		LocalDate day = range.getVoidedAt().atZone(zone).toLocalDate();
		for (long number = range.getStartNumber(); number <= range.getEndNumber(); number++) {
			if (!requireUnique("C100", reference, "own:" + range.getSeries() + "/" + number)) {
				continue;
			}
			rows.add(new Row(day, EXIT, range.getSeries(), number,
					identification(EXIT, OWN_ISSUE, null, VOIDED_NUMBER, range.getSeries(), number, null)));
		}
	}

	// ---- C100 --------------------------------------------------------------------------------------------------

	/** What the layout keeps of a cancelled document or a voided number: who, which, and its situation. */
	private static SpedRecord identification(String operation, String emitter, String participant, String situation,
			String series, long number, String accessKey) {
		return SpedRecord.of("C100", operation, emitter, participant, MODEL_NFE, situation, series,
				String.valueOf(number), accessKey);
	}

	private static SpedRecord regular(String operation, String emitter, String participant, String series, long number,
			String accessKey, LocalDate issued, LocalDate settled, Amounts a) {
		return SpedRecord.of("C100", operation, emitter, participant, MODEL_NFE, REGULAR, series,
				String.valueOf(number), accessKey, SpedValues.date(issued), SpedValues.date(settled),
				SpedValues.money(a.total()), "2", SpedValues.money(a.discount()), SpedValues.money(BigDecimal.ZERO),
				SpedValues.money(a.goods()), a.freightIndicator(), SpedValues.money(a.freight()),
				SpedValues.money(a.insurance()), SpedValues.money(a.otherExpenses()),
				SpedValues.money(a.icmsBase()), SpedValues.money(a.icms()), SpedValues.money(BigDecimal.ZERO),
				SpedValues.money(BigDecimal.ZERO), SpedValues.money(a.ipi()), SpedValues.money(a.pis()),
				SpedValues.money(a.cofins()), SpedValues.money(BigDecimal.ZERO), SpedValues.money(BigDecimal.ZERO));
	}

	private void book(String operation, BigDecimal icms) {
		regularDocuments++;
		if (operation.equals(EXIT)) {
			icmsDebit = icmsDebit.add(icms);
		} else {
			icmsCredit = icmsCredit.add(icms);
		}
	}

	// ---- 0150 --------------------------------------------------------------------------------------------------

	private void participate(Document document, String name, String stateRegistration) {
		boolean legalEntity = document.personType() == PersonType.COMPANY;
		String ie = stateRegistration != null && DIGITS.matcher(stateRegistration).matches() ? stateRegistration : null;
		participants.putIfAbsent(document.number(), SpedRecord.of("0150", document.number(), name, COUNTRY_BRAZIL,
				legalEntity ? document.number() : null, legalEntity ? null : document.number(), ie, null, null, null, null,
				null, null));
	}

	// ---- identification (0000, 0005, 0100) ---------------------------------------------------------------------

	private void validateIdentification() {
		Period period = command.period();
		if (!period.isWithinOneMonth()) {
			error("0000", "período", "DT_INI/DT_FIN: the EFD covers one calendar month, but the period spans "
					+ period.start() + " to " + period.end());
		}
		Taxpayer taxpayer = command.taxpayer();
		requireText("0000", "empresa", "NOME", taxpayer.legalName());
		if (taxpayer.municipalityCode() == null || !MUNICIPALITY_CODE.matcher(taxpayer.municipalityCode()).matches()) {
			error("0000", "empresa", "COD_MUN: the 7-digit IBGE municipality code is required");
		}
		if (company.getIe() == null || !DIGITS.matcher(company.getIe()).matches()) {
			error("0000", "empresa", "IE: the company's state registration must be numeric to file an EFD ICMS/IPI");
		}
		if (taxpayer.profile() == null) {
			error("0000", "empresa", "IND_PERFIL: the activity profile (A, B or C) is required");
		}
		if (taxpayer.activity() == null) {
			error("0000", "empresa", "IND_ATIV: the activity type is required");
		}
		if (taxpayer.zipCode() == null || !ZIP_CODE.matcher(taxpayer.zipCode()).matches()) {
			error("0005", "empresa", "CEP: the 8-digit zip code is required");
		}
		requireText("0005", "empresa", "END", company.getAddress());

		Accountant accountant = command.accountant();
		requireText("0100", "contabilista", "NOME", accountant.name());
		requireText("0100", "contabilista", "CRC", accountant.crc());
		try {
			Document.cpf(accountant.cpf());
		} catch (BusinessRuleException ex) {
			error("0100", "contabilista", "CPF: the accountant's CPF is missing or invalid");
		}
	}

	// ---- validation helpers ------------------------------------------------------------------------------------

	private boolean requireKey(String record, String reference, String accessKey) {
		if (accessKey == null || !ACCESS_KEY.matcher(accessKey).matches()) {
			error(record, reference, "CHV_NFE: the access key is missing or is not 44 digits");
			return false;
		}
		return true;
	}

	private boolean requireSeries(String record, String reference, String series) {
		if (series == null || series.isBlank() || series.trim().length() > 3) {
			error(record, reference, "SER: the series is missing or longer than 3 characters");
			return false;
		}
		return true;
	}

	private boolean requireNumber(String record, String reference, Long number) {
		if (number == null || number <= 0 || number > MAX_DOCUMENT_NUMBER) {
			error(record, reference, "NUM_DOC: the number is missing or does not fit 9 digits");
			return false;
		}
		return true;
	}

	private boolean requireUnique(String record, String reference, String key) {
		if (!seen.add(key)) {
			error(record, reference, "the document appears more than once in the period (" + key + ")");
			return false;
		}
		return true;
	}

	private void requireText(String record, String reference, String field, String value) {
		if (value == null || value.isBlank()) {
			error(record, reference, field + ": is required");
		}
	}

	private static Long parseNumber(String number) {
		if (number == null || !DIGITS.matcher(number.trim()).matches() || number.trim().length() > 9) {
			return null;
		}
		long value = Long.parseLong(number.trim());
		return value > 0 ? value : null;
	}

	private void error(String record, String reference, String message) {
		issues.add(new Issue(Severity.ERROR, record, reference, message));
	}

	private void warning(String record, String reference, String message) {
		issues.add(new Issue(Severity.WARNING, record, reference, message));
	}

	// ---- output ------------------------------------------------------------------------------------------------

	SpedValidationReport report() {
		List<Issue> all = new ArrayList<>(issues);
		if (!participants.isEmpty()) {
			all.add(new Issue(Severity.WARNING, "0150", participants.size() + " participante(s)",
					"COD_MUN, END, NUM and BAIRRO are not held for customers and suppliers and go out empty"));
		}
		if (regularDocuments > 0) {
			all.add(new Issue(Severity.WARNING, "C170/C190", regularDocuments + " documento(s)",
					"items (C170, 0200) and the analytic summary (C190) are not generated: CST, ICMS rate and "
							+ "per-item unit/NCM are not held"));
		}
		if (receivedWithoutIcmsBase > 0) {
			all.add(new Issue(Severity.WARNING, "C100", receivedWithoutIcmsBase + " NFe recebida(s)",
					"VL_BC_ICMS is not held for received NFe (only the ICMS amount) and goes out empty"));
		}
		all.add(new Issue(Severity.WARNING, "E110", "apuração",
				"VL_SLD_CREDOR_ANT (credit carried from the previous period) is not tracked and is reported as 0,00"));
		return new SpedValidationReport(all);
	}

	/** Every block of the layout in file order; those with nothing to report are empty. */
	List<SpedBlock> blocks() {
		Taxpayer taxpayer = command.taxpayer();
		Period period = command.period();
		SpedRecord header = SpedRecord.of("0000", LAYOUT_VERSION, command.finality().code(),
				SpedValues.date(period.start()), SpedValues.date(period.end()), taxpayer.legalName(),
				company.getCnpj().number(), null, company.getState(), company.getIe(), taxpayer.municipalityCode(),
				company.getIm(), null, taxpayer.profile().name(), taxpayer.activity().code());
		List<SpedRecord> identification = new ArrayList<>();
		identification.add(SpedRecord.of("0005", taxpayer.tradeName(), taxpayer.zipCode(), company.getAddress(),
				taxpayer.number(), null, taxpayer.neighborhood(), digits(company.getPhone()), null,
				company.getIssuingEmail()));
		Accountant accountant = command.accountant();
		identification.add(SpedRecord.of("0100", accountant.name(), digits(accountant.cpf()), accountant.crc(), null,
				null, null, null, null, null, null, null, accountant.email(), null));
		identification.addAll(participants.values());

		List<SpedRecord> documents = rows.stream().sorted(ROW_ORDER).map(Row::record).toList();
		List<SpedRecord> assessment = List.of(
				SpedRecord.of("E100", SpedValues.date(period.start()), SpedValues.date(period.end())), icmsAssessment());
		List<SpedRecord> indicators = List.of(SpedRecord.of("1010", "N", "N", "N", "N", "N", "N", "N", "N", "N", "N",
				"N", "N", "N"));

		List<SpedBlock> blocks = new ArrayList<>();
		blocks.add(new SpedBlock('0', List.of(header), identification));
		blocks.add(new SpedBlock('B', List.of()));
		blocks.add(new SpedBlock('C', documents));
		blocks.add(new SpedBlock('D', List.of()));
		blocks.add(new SpedBlock('E', assessment));
		blocks.add(new SpedBlock('G', List.of()));
		blocks.add(new SpedBlock('H', List.of()));
		blocks.add(new SpedBlock('K', List.of()));
		blocks.add(new SpedBlock('1', indicators));
		return blocks;
	}

	/** E110: debits of the exits against credits of the entries; a negative balance is a credit to carry forward. */
	private SpedRecord icmsAssessment() {
		BigDecimal balance = icmsDebit.subtract(icmsCredit);
		BigDecimal payable = balance.max(BigDecimal.ZERO);
		BigDecimal carried = balance.min(BigDecimal.ZERO).negate();
		BigDecimal zero = BigDecimal.ZERO;
		return SpedRecord.of("E110", SpedValues.money(icmsDebit), SpedValues.money(zero), SpedValues.money(zero),
				SpedValues.money(zero), SpedValues.money(icmsCredit), SpedValues.money(zero), SpedValues.money(zero),
				SpedValues.money(zero), SpedValues.money(zero), SpedValues.money(payable), SpedValues.money(zero),
				SpedValues.money(payable), SpedValues.money(carried), SpedValues.money(zero));
	}

	private static String digits(String value) {
		return value == null ? null : Document.digitsOnly(value);
	}

	private record Row(LocalDate date, String operation, String series, long number, SpedRecord record) {
	}

	private record Amounts(BigDecimal total, BigDecimal discount, BigDecimal goods, String freightIndicator,
			BigDecimal freight, BigDecimal insurance, BigDecimal otherExpenses, BigDecimal icmsBase, BigDecimal icms,
			BigDecimal ipi, BigDecimal pis, BigDecimal cofins) {
	}
}
