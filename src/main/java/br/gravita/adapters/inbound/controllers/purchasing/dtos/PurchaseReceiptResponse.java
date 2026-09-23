package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.util.UUID;

public record PurchaseReceiptResponse(UUID id) {
	public static PurchaseReceiptResponse from(PurchaseReceiptId id) {
		return new PurchaseReceiptResponse(id.value());
	}
}
