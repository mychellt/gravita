package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SalesOrderView(UUID id, UUID originQuoteId, UUID customerId, SalesOrderStatus status,
		List<SalesOrderItem> items, BigDecimal totalValue, UUID approvedBy, UUID alcadaId) {

	public static SalesOrderView from(SalesOrder order) {
		return new SalesOrderView(order.getId().value(), order.getOriginQuoteId().value(), order.getCustomerId(),
				order.getStatus(), order.getItems(), order.totalValue(), order.getApprovedBy(),
				order.getAlcadaId());
	}
}
