package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.Lot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Projection of a {@link Lot} returned by {@link CheckExpiringLotsUseCase}
 * (UC-M5-10, AC1). {@code remainingQuantity} mirrors the lot's current
 * quantity at query time.
 */
public record ExpiringLotView(UUID productId, UUID warehouseId, String lotCode, LocalDate expiryDate,
		BigDecimal remainingQuantity) {

	public static ExpiringLotView from(Lot lot) {
		return new ExpiringLotView(lot.getProductId(), lot.getWarehouseId(), lot.getCode(), lot.getExpiryDate(),
				lot.getQuantity());
	}
}
