package br.gravita.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builds the NFe the fiscal books (UC-M2-13) are tested over. */
public final class LivrosFiscaisFixtures {

	private LivrosFiscaisFixtures() {
	}

	/**
	 * An NFe {@code company} issued for 100.00 of goods plus 15.00 of freight, with ICMS 18.00, IPI 5.00, PIS 1.65
	 * and COFINS 7.60 stated. {@code authorizedAt} is {@code null} for a document SEFAZ never authorized.
	 */
	public static NfeDocument issuedNfe(final CompanyId company, final NfeDocumentStatus status, final String cfop, final String series,
			final long number, final Instant authorizedAt) {
		final List<TaxLineBreakdown> lines = new ArrayList<>();
		lines.add(tax(TaxType.ICMS, "18.00"));
		lines.add(tax(TaxType.IPI, "5.00"));
		lines.add(tax(TaxType.PIS, "1.65"));
		lines.add(tax(TaxType.COFINS, "7.60"));
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, "rice", lines);
		final NfeItem item = new NfeItem(UUID.randomUUID(), "Arroz", BigDecimal.TEN, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		final NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81",
				PersonType.COMPANY, "Cliente SA", "123456789", "RJ");
		final Instant createdAt = authorizedAt == null ? Instant.parse("2020-01-01T00:00:00Z") : authorizedAt.minusSeconds(60);
		final Instant cancelledAt = status == NfeDocumentStatus.CANCELLED ? authorizedAt.plusSeconds(3600) : null;
		return NfeDocument.builder()
				.id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(company)
				.originSalesOrderId(null)
				.naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop(cfop))
				.recipient(recipient)
				.items(List.of(item))
				.freight(new BigDecimal("15.00"))
				.insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO)
				.transport(null)
				.referencedAccessKey(null)
				.additionalInfo(null)
				.taxTotals(TaxCalculationTotals.from(List.of(breakdown)))
				.status(status)
				.createdAt(createdAt)
				.documentSeries(series)
				.documentNumber(number)
				.accessKey("35" + String.format("%042d", number + series.hashCode() % 1000 * 1_000_000L))
				.sefazProtocol("protocol")
				.contingencyMode(false)
				.rejectionReason(null)
				.xmlStorageRef(null)
				.danfeStorageRef(null)
				.correctionLetters(List.of())
				.authorizedAt(authorizedAt)
				.cancellationJustification(cancelledAt == null ? null : "Cancelada a pedido")
				.cancelledAt(cancelledAt)
				.build();
	}

	/** The ICMS, IPI, PIS and COFINS amounts stated on a received NFe's totals. */
	public record Taxes(String icms, String ipi, String pis, String cofins) {
	}

	/**
	 * An NFe {@code company} received, two items under {@code cfopA} and {@code cfopB}, whose ICMS, IPI, PIS and
	 * COFINS are stated on its totals.
	 */
	public static InboundNfe receivedNfe(final CompanyId company, final String series, final String number, final String supplier,
			final String cfopA, final String cfopB, final String total, final Taxes taxes, final Instant issuedAt) {
		final BigDecimal totalValue = new BigDecimal(total);
		final InboundNfeItem a = new InboundNfeItem("SKU-A", "Item A", "73181500", cfopA, "UN", BigDecimal.ONE, totalValue,
				totalValue, new BigDecimal(taxes.icms()), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		final InboundNfeItem b = new InboundNfeItem("SKU-B", "Item B", "73181500", cfopB, "UN", BigDecimal.ONE,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		final InboundNfeTotals totals = new InboundNfeTotals(totalValue, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, new BigDecimal(taxes.icms()), new BigDecimal(taxes.ipi()), new BigDecimal(taxes.pis()),
				new BigDecimal(taxes.cofins()), totalValue);
		return InboundNfe.builder()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(company)
				.accessKey("35" + String.format("%042d", Math.abs((long) (series + number + supplier).hashCode())))
				.series(series)
				.number(number)
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName(supplier)
				.issuedAt(issuedAt)
				.items(List.of(a, b))
				.totals(totals)
				.xmlStorageRef("xml-ref")
				.status(InboundNfeStatus.PENDING_CONFERENCE)
				.importedAt(issuedAt)
				.build();
	}

	private static TaxLineBreakdown tax(final TaxType type, final String amount) {
		return new TaxLineBreakdown(type, new BigDecimal("100.00"), new BigDecimal("1"), new BigDecimal(amount),
				new BigDecimal(amount), false, null);
	}
}
