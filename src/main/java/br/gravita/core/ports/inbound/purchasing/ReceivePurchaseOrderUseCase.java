package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;

/**
 * UC-M6-06: opens a {@code PurchaseReceipt} against an open order, recording
 * the physically received quantities per item (total or partial). This is the
 * physical-conference step only - it doesn't update stock or generate a
 * payable, that happens once {@code ConfirmPurchaseReceiptUseCase} (UC-M6-08)
 * is called.
 */
public interface ReceivePurchaseOrderUseCase {
	PurchaseReceiptId execute(ReceivePurchaseOrderCommand command);
}
