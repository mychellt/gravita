package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseRequestId;

public interface CreatePurchaseRequestUseCase {
	PurchaseRequestId execute(CreatePurchaseRequestCommand command);
}
