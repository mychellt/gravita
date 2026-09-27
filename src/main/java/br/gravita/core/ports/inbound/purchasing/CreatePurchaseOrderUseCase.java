package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseOrderId;

public interface CreatePurchaseOrderUseCase {
	PurchaseOrderId execute(CreatePurchaseOrderCommand command);
}
