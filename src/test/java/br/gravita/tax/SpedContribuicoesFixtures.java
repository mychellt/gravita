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
	public static NfeDocument issuedNfe(final CompanyId company, final String cfop, final String series, final long number,
			final Instant authorizedAt, final String pisRate, final String cofinsRate) {
		final List<TaxLineBreakdown> lines = new ArrayList<>();
		lines.add(line(TaxType.ICMS, "100.00", "18", "18.00"));
		if (pisRate != null) {
			lines.add(line(TaxType.PIS, "100.00", pisRate, percentOf("100.00", pisRate)));
		}
		if (cofinsRate != null) {
			lines.add(line(TaxType.COFINS, "100.00", cofinsRate, percentOf("100.00", cofinsRate)));
		}
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, "rice", lines);
		final NfeItem item = new NfeItem(UUID.fromString("00000000-0000-0000-0000-000000000042"), "Arroz",
				BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, breakdown);
		final NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.444.777/0001-61",
				PersonType.COMPANY, "Cliente SA", "123456789", "RJ");
		return NfeDocument.builder()
				.id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(company)
				.originSalesOrderId(null)
				.naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop(cfop))
				.recipient(recipient)
				.items(List.of(item))
				.freight(BigDecimal.ZERO)
				.insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO)
				.transport(null)
				.referencedAccessKey(null)
				.additionalInfo(null)
				.taxTotals(TaxCalculationTotals.from(List.of(breakdown)))
				.status(NfeDocumentStatus.AUTHORIZED)
				.createdAt(authorizedAt.minusSeconds(60))
				.documentSeries(series)
				.documentNumber(number)
				.accessKey(String.format("%044d", number))
				.sefazProtocol("protocol")
				.contingencyMode(false)
				.rejectionReason(null)
				.xmlStorageRef(null)
				.danfeStorageRef(null)
				.correctionLetters(List.of())
				.authorizedAt(authorizedAt)
				.cancellationJustification(null)
				.cancelledAt(null)
				.build();
	}

	/**
	 * An NFe {@code company} received from {@code supplierCnpj}: one item of {@code value} under {@code cfop}, on
	 * which the supplier stated {@code pis} and {@code cofins}.
	 */
	public static InboundNfe receivedNfe(final CompanyId company, final InboundNfeStatus status, final String number,
			final String supplierCnpj, final String supplierName, final String cfop, final String value, final String pis, final String cofins,
			final Instant issuedAt) {
		final BigDecimal total = new BigDecimal(value);
		final InboundNfeItem item = new InboundNfeItem("SKU-1", "Parafuso", "73181500", cfop, "UN", BigDecimal.ONE, total,
				total, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(pis), new BigDecimal(cofins));
		final InboundNfeTotals totals = new InboundNfeTotals(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(pis), new BigDecimal(cofins),
				total);
		return InboundNfe.builder()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(company)
				.accessKey("35" + String.format("%042d", Long.parseLong(number)))
				.series("1")
				.number(number)
				.supplierDocument(Document.cnpj(supplierCnpj))
				.supplierName(supplierName)
				.issuedAt(issuedAt)
				.items(List.of(item))
				.totals(totals)
				.xmlStorageRef("xml-ref")
				.status(status)
				.importedAt(issuedAt)
				.build();
	}

	private static TaxLineBreakdown line(final TaxType type, final String base, final String rate, final String amount) {
		return new TaxLineBreakdown(type, new BigDecimal(base), new BigDecimal(rate), new BigDecimal(amount),
				new BigDecimal(amount), false, null);
	}

	private static String percentOf(final String base, final String rate) {
		return new BigDecimal(base).multiply(new BigDecimal(rate)).movePointLeft(2).toPlainString();
	}
}
