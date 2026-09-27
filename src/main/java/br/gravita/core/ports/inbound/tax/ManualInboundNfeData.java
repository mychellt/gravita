package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.time.Instant;
import java.util.List;

/**
 * UC-M2-09: everything needed to build an {@code InboundNfe} by hand, for a
 * supplier that didn't provide an XML. Carries its own {@code accessKey} -
 * read off the supplier's printed DANFE - since it's required by
 * {@code InboundNfe} regardless of entry method.
 */
public record ManualInboundNfeData(
		String accessKey,
		String series,
		String number,
		Document supplierDocument,
		String supplierName,
		Instant issuedAt,
		List<InboundNfeItem> items,
		InboundNfeTotals totals) {
}
