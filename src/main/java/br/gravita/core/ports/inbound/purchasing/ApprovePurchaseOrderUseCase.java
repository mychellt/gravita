package br.gravita.core.ports.inbound.purchasing;

public interface ApprovePurchaseOrderUseCase {
	void execute(ApprovePurchaseOrderCommand command);
}
