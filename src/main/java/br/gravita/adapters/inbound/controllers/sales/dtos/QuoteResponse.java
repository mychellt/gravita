package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.QuoteStatus;
import br.gravita.core.ports.inbound.sales.QuoteView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(UUID id, UUID customerId, UUID salespersonId, QuoteStatus status,
		List<ItemResponse> items, LocalDate validUntil, BigDecimal totalValue) {

	public static QuoteResponse from(final QuoteView view) {
		return new QuoteResponse(view.id(), view.customerId(), view.salespersonId(), view.status(),
				view.items().stream().map(ItemResponse::from).toList(), view.validUntil(), view.totalValue());
	}

	public record ItemResponse(UUID productOrServiceId, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal discount, BigDecimal lineTotal) {

		static ItemResponse from(final QuoteItem item) {
			return new ItemResponse(item.productOrServiceId(), item.quantity(), item.unitPrice(), item.discount(),
					item.lineTotal());
		}
	}
}
