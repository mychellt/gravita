package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseOrderId;
import java.util.UUID;

public record PurchaseOrderResponse(UUID id) {
	public static PurchaseOrderResponse from(PurchaseOrderId id) {
		return new PurchaseOrderResponse(id.value());
	}
}
