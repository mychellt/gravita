package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseRequestId;
import java.util.UUID;

public record PurchaseRequestResponse(UUID id) {
	public static PurchaseRequestResponse from(PurchaseRequestId id) {
		return new PurchaseRequestResponse(id.value());
	}
}
