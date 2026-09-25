package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.ExpiringLotView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpiringLotResponse(UUID productId, UUID warehouseId, String lotCode, LocalDate expiryDate,
		BigDecimal remainingQuantity) {

	public static ExpiringLotResponse from(ExpiringLotView view) {
		return new ExpiringLotResponse(view.productId(), view.warehouseId(), view.lotCode(), view.expiryDate(),
				view.remainingQuantity());
	}
}
