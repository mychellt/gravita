package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteView(UUID id, UUID customerId, UUID salespersonId, QuoteStatus status, List<QuoteItem> items,
		LocalDate validUntil, BigDecimal totalValue) {

	public static QuoteView from(Quote quote) {
		return new QuoteView(quote.getId().value(), quote.getCustomerId(), quote.getSalespersonId(),
				quote.getStatus(), quote.getItems(), quote.getValidUntil(), quote.totalValue());
	}
}
