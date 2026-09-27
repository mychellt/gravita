package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseReturnId;

public interface ReturnToSupplierUseCase {
	PurchaseReturnId execute(ReturnToSupplierCommand command);
}
