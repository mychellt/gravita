package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseReturnId;
import java.util.UUID;

public record PurchaseReturnResponse(UUID id) {
	public static PurchaseReturnResponse from(final PurchaseReturnId id) {
		return new PurchaseReturnResponse(id.value());
	}
}
