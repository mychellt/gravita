package br.gravita.core.ports.inbound.purchasing;

public interface ConfirmPurchaseReceiptUseCase {
	void execute(ConfirmPurchaseReceiptCommand command);
}
