package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrderId;

/**
 * UC-M6-04: converts a quoted (or directly requested) {@code PurchaseRequest}
 * into a {@code PurchaseOrder} against one chosen supplier.
 */
public interface CreatePurchaseOrderUseCase {
	PurchaseOrderId execute(CreatePurchaseOrderCommand command);
}
