package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.PurchaseRequestId;

/**
 * Entry point of the purchasing module (UC-M6-01). Called directly by a user
 * action, or by inventory's {@code SuggestReorderUseCase} (M5) and an
 * approved sales order (M7) once those modules exist - both are external
 * callers of this same port, not separate use cases.
 */
public interface CreatePurchaseRequestUseCase {
	PurchaseRequestId execute(CreatePurchaseRequestCommand command);
}
