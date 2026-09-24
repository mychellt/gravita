package br.gravita.core.usercases.tax;

import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.time.Instant;
import java.util.List;

/**
 * Raw values read off a supplier's NFe XML, before {@link ImportSupplierNfeXmlService}
 * applies domain semantics (e.g. {@code supplierCnpj} becomes a validated
 * {@code Document}).
 */
public record ParsedSupplierNfe(
		String accessKey,
		String series,
		String number,
		String supplierCnpj,
		String supplierName,
		Instant issuedAt,
		List<InboundNfeItem> items,
		InboundNfeTotals totals) {
}
