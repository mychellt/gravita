package br.gravita.core.usercases.tax;

import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Incidence;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Item;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Levy;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Operation;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Party;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Turns the NFe a company issued and received into {@link SpedContribuicoesDocument}s, deciding on the way what each
 * item does to PIS and COFINS under the company's {@link Incidence}.
 *
 * <p><b>Sales</b> are levied as the NFe states: the base, the rate and the amount of its PIS/COFINS line, so the
 * assessment reconciles with what was invoiced. An item with no such line (or a zero one) bears nothing (CST
 * {@code 49}); one at the regime's general rate is CST {@code 01}, at any other rate CST {@code 02}.
 *
 * <p><b>Purchases</b> earn a credit only under the non-cumulative regime, and only when it is evident the goods were
 * bought for resale or as an input (their CFOP says so) and the supplier's NFe itself bore the contribution (so
 * zero-rated, suspended and monophasic purchases earn none). The credit is the item's value at the buyer's general
 * rate - not the supplier's amount, which depends on the supplier's own regime - and is CST {@code 50}. Every other
 * purchase is CST {@code 70}, no credit.
 */
final class SpedContribuicoesDocuments {

	static final String UNTAXED_SALE_CST = "49";
	static final String UNCREDITED_PURCHASE_CST = "70";
	private static final String BASIC_RATE_SALE_CST = "01";
	private static final String OTHER_RATE_SALE_CST = "02";
	private static final String CREDITED_PURCHASE_CST = "50";
	private static final String RESALE = "01";
	private static final String INPUT = "02";
	private static final String DEFAULT_UNIT = "UN";
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final Incidence incidence;
	private final ZoneId zone;

	SpedContribuicoesDocuments(final Incidence incidence, final ZoneId zone) {
		this.incidence = incidence;
		this.zone = zone;
	}

	/** An NFe the company issued: a sale under an exit CFOP (5, 6, 7), a purchase of its own under an entry one (1, 2, 3). */
	SpedContribuicoesDocument fromIssued(final NfeDocument nfe) {
		final String cfop = nfe.getCfop().code();
		final Operation operation = switch (cfop.charAt(0)) {
			case '1', '2', '3' -> Operation.ENTRY;
			default -> Operation.EXIT;
		};
		final List<Item> items = nfe.getItems().stream().map(item -> issuedItem(item, operation, cfop)).toList();
		final Document recipient = nfe.getRecipient().document();
		final BigDecimal pis = stated(nfe, SpedTax.PIS);
		final BigDecimal cofins = stated(nfe, SpedTax.COFINS);
		return new SpedContribuicoesDocument(operation, true,
				party(recipient, nfe.getRecipient().name()), nfe.getDocumentSeries(),
				nfe.getDocumentNumber() == null ? null : String.valueOf(nfe.getDocumentNumber()), nfe.getAccessKey(),
				nfe.getAuthorizedAt().atZone(zone).toLocalDate(), money(nfe.getDocumentTotal()),
				money(nfe.getItems().stream().map(NfeItem::discount).reduce(BigDecimal.ZERO, BigDecimal::add)),
				money(nfe.getItems().stream().map(NfeItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)),
				money(nfe.getFreight()), money(nfe.getInsurance()), money(nfe.getOtherExpenses()), pis, cofins, items);
	}

	/** An NFe the company received: always a purchase. */
	SpedContribuicoesDocument fromReceived(final InboundNfe nfe) {
		final Document supplier = nfe.getSupplierDocument();
		final InboundNfeTotals totals = nfe.getTotals();
		final List<Item> items = nfe.getItems().stream().map(item -> receivedItem(item, supplier)).toList();
		final LocalDate date = nfe.getIssuedAt().atZone(zone).toLocalDate();
		return new SpedContribuicoesDocument(Operation.ENTRY, false, party(supplier, nfe.getSupplierName()),
				nfe.getSeries(), nfe.getNumber(), nfe.getAccessKey(), date, money(totals.totalValue()),
				money(totals.discountValue()), money(totals.productsValue()), money(totals.freightValue()),
				money(totals.insuranceValue()), money(totals.otherExpensesValue()), money(totals.pisValue()),
				money(totals.cofinsValue()), items);
	}

	private Item issuedItem(final NfeItem item, final Operation operation, final String cfop) {
		final BigDecimal base = item.lineTotal();
		return new Item(item.productId().toString(), item.description(), null, DEFAULT_UNIT, item.quantity(),
				money(item.subtotal()), money(item.discount()), cfop,
				levy(SpedTax.PIS, operation, item, base, cfop), levy(SpedTax.COFINS, operation, item, base, cfop));
	}

	private Item receivedItem(final InboundNfeItem item, final Document supplier) {
		final String unit = item.unit() == null || item.unit().isBlank() ? DEFAULT_UNIT : item.unit();
		return new Item(supplier.number() + "-" + item.supplierProductCode(), item.description(), item.ncm(), unit,
				item.quantity(), money(item.totalValue()), BigDecimal.ZERO.setScale(2), item.cfop(),
				purchase(SpedTax.PIS, item.totalValue(), item.pisValue(), item.cfop()),
				purchase(SpedTax.COFINS, item.totalValue(), item.cofinsValue(), item.cfop()));
	}

	private Levy levy(final SpedTax tax, final Operation operation, final NfeItem item, final BigDecimal base, final String cfop) {
		final Optional<TaxLineBreakdown> line = item.taxBreakdown().taxLines().stream()
				.filter(candidate -> candidate.taxType() == tax.taxType()).findFirst();
		return operation == Operation.EXIT ? sale(tax, line.orElse(null), base)
				: purchase(tax, base, line.map(TaxLineBreakdown::finalAmount).orElse(null), cfop);
	}

	private Levy sale(final SpedTax tax, final TaxLineBreakdown line, final BigDecimal itemRevenue) {
		if (line == null || line.finalAmount() == null || line.finalAmount().signum() <= 0) {
			return Levy.none(UNTAXED_SALE_CST);
		}
		final BigDecimal basic = tax.basicRate(incidence);
		final BigDecimal rate = rate(line.ratePercentage() == null ? basic : line.ratePercentage());
		final BigDecimal base = line.base() == null ? itemRevenue : line.base();
		final String cst = rate.compareTo(rate(basic)) == 0 ? BASIC_RATE_SALE_CST : OTHER_RATE_SALE_CST;
		return new Levy(cst, money(base), rate, money(line.finalAmount()), null);
	}

	/** {@code stated} is the contribution the supplier's NFe bore on the item, {@code null} if it states none. */
	private Levy purchase(final SpedTax tax, final BigDecimal base, final BigDecimal stated, final String cfop) {
		final String nature = creditNature(cfop);
		if (incidence == Incidence.CUMULATIVE || nature == null || stated == null || stated.signum() <= 0) {
			return Levy.none(UNCREDITED_PURCHASE_CST);
		}
		final BigDecimal rate = rate(tax.basicRate(Incidence.NON_CUMULATIVE));
		final BigDecimal credit = money(base.multiply(rate).divide(HUNDRED));
		if (credit.signum() <= 0) {
			return Levy.none(UNCREDITED_PURCHASE_CST);
		}
		return new Levy(CREDITED_PURCHASE_CST, money(base), rate, credit, nature);
	}

	/**
	 * {@code NAT_BC_CRED} of a purchase whose CFOP says it is for resale ({@code x102}, {@code x403}, {@code x405}) or
	 * as an input ({@code x101}, {@code x401}); {@code null} for anything else, which earns no credit.
	 */
	private static String creditNature(final String cfop) {
		final String digits = cfop == null ? "" : Document.digitsOnly(cfop);
		if (digits.length() != 4 || "123".indexOf(digits.charAt(0)) < 0) {
			return null;
		}
		return switch (digits.substring(1)) {
			case "102", "403", "405" -> RESALE;
			case "101", "401" -> INPUT;
			default -> null;
		};
	}

	private static BigDecimal stated(final NfeDocument nfe, final SpedTax tax) {
		return money(nfe.getTaxTotals().byTaxType().getOrDefault(tax.taxType(), BigDecimal.ZERO));
	}

	private static Party party(final Document document, final String name) {
		final boolean company = document.personType() == PersonType.COMPANY;
		return new Party(document.number(), name, company ? document.number() : null,
				company ? null : document.number());
	}

	static BigDecimal money(final BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	static BigDecimal rate(final BigDecimal percent) {
		return percent.setScale(4, RoundingMode.HALF_UP);
	}
}
