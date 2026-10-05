package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SalesOrderResponse(UUID id, UUID originQuoteId, UUID customerId, UUID salespersonId,
		SalesOrderStatus status, List<ItemResponse> items, BigDecimal totalValue, UUID approvedBy, UUID alcadaId) {

	public static SalesOrderResponse from(final SalesOrderView view) {
		return new SalesOrderResponse(view.id(), view.originQuoteId(), view.customerId(), view.salespersonId(),
				view.status(), view.items().stream().map(ItemResponse::from).toList(), view.totalValue(),
				view.approvedBy(), view.alcadaId());
	}

	public record ItemResponse(UUID productOrServiceId, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal discount, BigDecimal lineTotal) {

		static ItemResponse from(final SalesOrderItem item) {
			return new ItemResponse(item.productOrServiceId(), item.quantity(), item.unitPrice(), item.discount(),
					item.lineTotal());
		}
	}
}
