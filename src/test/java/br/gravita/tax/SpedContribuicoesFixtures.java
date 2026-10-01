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

/** Builds the NFe the EFD Contribuições (UC-M2-12) is tested over, with PIS/COFINS lines whose rate, base and amount agree. */
public final class SpedContribuicoesFixtures {

	private SpedContribuicoesFixtures() {
	}

	/**
	 * An authorized NFe {@code company} issued for 10 x 10.00 of goods, with ICMS stated and PIS/COFINS lines at
	 * {@code pisRate}/{@code cofinsRate} percent over the 100.00 (a {@code null} rate states no line at all).
	 */
	public static NfeDocument issuedNfe(CompanyId company, String cfop, String series, long number,
			Instant authorizedAt, String pisRate, String cofinsRate) {
		List<TaxLineBreakdown> lines = new ArrayList<>();
		lines.add(line(TaxType.ICMS, "100.00", "18", "18.00"));
		if (pisRate != null) {
			lines.add(line(TaxType.PIS, "100.00", pisRate, percentOf("100.00", pisRate)));
		}
		if (cofinsRate != null) {
			lines.add(line(TaxType.COFINS, "100.00", cofinsRate, percentOf("100.00", cofinsRate)));
		}
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, "rice", lines);
		NfeItem item = new NfeItem(UUID.fromString("00000000-0000-0000-0000-000000000042"), "Arroz",
				BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.444.777/0001-61",
				PersonType.COMPANY, "Cliente SA", "123456789", "RJ");
		return NfeDocument.of(NfeDocumentId.of(UUID.randomUUID()), company, null, NaturezaOperacao.VENDA,
				new Cfop(cfop), recipient, List.of(item), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
				null, null, TaxCalculationTotals.from(List.of(breakdown)), NfeDocumentStatus.AUTHORIZED,
				authorizedAt.minusSeconds(60), series, number, String.format("%044d", number), "protocol", false,
				null, null, null, List.of(), authorizedAt, null, null);
	}

	/**
	 * An NFe {@code company} received from {@code supplierCnpj}: one item of {@code value} under {@code cfop}, on
	 * which the supplier stated {@code pis} and {@code cofins}.
	 */
	public static InboundNfe receivedNfe(CompanyId company, InboundNfeStatus status, String number,
			String supplierCnpj, String supplierName, String cfop, String value, String pis, String cofins,
			Instant issuedAt) {
		BigDecimal total = new BigDecimal(value);
		InboundNfeItem item = new InboundNfeItem("SKU-1", "Parafuso", "73181500", cfop, "UN", BigDecimal.ONE, total,
				total, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(pis), new BigDecimal(cofins));
		InboundNfeTotals totals = new InboundNfeTotals(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(pis), new BigDecimal(cofins),
				total);
		return InboundNfe.of(InboundNfeId.of(UUID.randomUUID()), company, "35" + String.format("%042d", Long.parseLong(number)),
				"1", number, Document.cnpj(supplierCnpj), supplierName, issuedAt, List.of(item), totals, "xml-ref",
				status, issuedAt);
	}

	private static TaxLineBreakdown line(TaxType type, String base, String rate, String amount) {
		return new TaxLineBreakdown(type, new BigDecimal(base), new BigDecimal(rate), new BigDecimal(amount),
				new BigDecimal(amount), false, null);
	}

	private static String percentOf(String base, String rate) {
		return new BigDecimal(base).multiply(new BigDecimal(rate)).movePointLeft(2).toPlainString();
	}
}
