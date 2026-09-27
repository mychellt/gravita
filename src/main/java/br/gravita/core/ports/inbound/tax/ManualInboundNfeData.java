package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.time.Instant;
import java.util.List;

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
