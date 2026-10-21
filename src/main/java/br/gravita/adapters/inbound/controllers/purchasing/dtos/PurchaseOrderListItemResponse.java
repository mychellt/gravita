package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.ports.inbound.purchasing.PurchaseOrderListItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderListItemResponse(UUID id, UUID supplierId, String supplierName, PurchaseOrderStatus status,
		boolean approvalRequired, UUID approvedBy, LocalDateTime createdAt, BigDecimal totalValue, List<ItemResponse> items) {

	public record ItemResponse(UUID productId, BigDecimal quantity, BigDecimal unitPrice) {
	}

	public static PurchaseOrderListItemResponse from(final PurchaseOrderListItem item) {
		final PurchaseOrder order = item.order();
		return new PurchaseOrderListItemResponse(order.getId().value(), order.getSupplierId().value(), item.supplierName(),
				order.getStatus(), order.isApprovalRequired(), order.getApprovedBy(), item.createdAt(), order.totalValue(),
				order.getItems().stream().map(i -> new ItemResponse(i.productId(), i.quantity(), i.unitPrice())).toList());
	}
}
