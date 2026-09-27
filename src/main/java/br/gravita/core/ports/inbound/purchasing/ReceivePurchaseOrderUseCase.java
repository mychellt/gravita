package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;

public interface ReceivePurchaseOrderUseCase {
	PurchaseReceiptId execute(ReceivePurchaseOrderCommand command);
}
