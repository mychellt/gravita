package br.gravita.core.ports.inbound.purchasing;

/**
 * UC-M6-08: finalizes a {@code PurchaseReceipt} after physical conference (and,
 * where applicable, NF-e reconciliation), triggering a stock entry per received
 * item and accounts-payable generation from the receipt's installment terms.
 */
public interface ConfirmPurchaseReceiptUseCase {
	void execute(ConfirmPurchaseReceiptCommand command);
}
