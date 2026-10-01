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
	public static NfeDocument issuedNfe(CompanyId company, NfeDocumentStatus status, String cfop, String series,
			long number, Instant authorizedAt) {
		List<TaxLineBreakdown> lines = new ArrayList<>();
		lines.add(tax(TaxType.ICMS, "18.00"));
		lines.add(tax(TaxType.IPI, "5.00"));
		lines.add(tax(TaxType.PIS, "1.65"));
		lines.add(tax(TaxType.COFINS, "7.60"));
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, "rice", lines);
		NfeItem item = new NfeItem(UUID.randomUUID(), "Arroz", BigDecimal.TEN, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81",
				PersonType.COMPANY, "Cliente SA", "123456789", "RJ");
		Instant createdAt = authorizedAt == null ? Instant.parse("2020-01-01T00:00:00Z") : authorizedAt.minusSeconds(60);
		Instant cancelledAt = status == NfeDocumentStatus.CANCELLED ? authorizedAt.plusSeconds(3600) : null;
		return NfeDocument.of(NfeDocumentId.of(UUID.randomUUID()), company, null, NaturezaOperacao.VENDA,
				new Cfop(cfop), recipient, List.of(item), new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO,
				null, null, null, TaxCalculationTotals.from(List.of(breakdown)), status, createdAt, series, number,
				"35" + String.format("%042d", number + series.hashCode() % 1000 * 1_000_000L), "protocol", false, null,
				null, null, List.of(), authorizedAt, cancelledAt == null ? null : "Cancelada a pedido", cancelledAt);
	}

	/**
	 * An NFe {@code company} received, two items under {@code cfopA} and {@code cfopB}, whose ICMS, IPI, PIS and
	 * COFINS are stated on its totals.
	 */
	public static InboundNfe receivedNfe(CompanyId company, String series, String number, String supplier,
			String cfopA, String cfopB, String total, String icms, String ipi, String pis, String cofins,
			Instant issuedAt) {
		BigDecimal totalValue = new BigDecimal(total);
		InboundNfeItem a = new InboundNfeItem("SKU-A", "Item A", "73181500", cfopA, "UN", BigDecimal.ONE, totalValue,
				totalValue, new BigDecimal(icms), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		InboundNfeItem b = new InboundNfeItem("SKU-B", "Item B", "73181500", cfopB, "UN", BigDecimal.ONE,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		InboundNfeTotals totals = new InboundNfeTotals(totalValue, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, new BigDecimal(icms), new BigDecimal(ipi), new BigDecimal(pis),
				new BigDecimal(cofins), totalValue);
		return InboundNfe.of(InboundNfeId.of(UUID.randomUUID()), company,
				"35" + String.format("%042d", Math.abs((long) (series + number + supplier).hashCode())), series,
				number, Document.cnpj("11222333000181"), supplier, issuedAt, List.of(a, b), totals, "xml-ref",
				InboundNfeStatus.PENDING_CONFERENCE, issuedAt);
	}

	private static TaxLineBreakdown tax(TaxType type, String amount) {
		return new TaxLineBreakdown(type, new BigDecimal("100.00"), new BigDecimal("1"), new BigDecimal(amount),
				new BigDecimal(amount), false, null);
	}
}
