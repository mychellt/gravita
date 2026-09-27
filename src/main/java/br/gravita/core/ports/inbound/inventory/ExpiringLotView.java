package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.Lot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpiringLotView(UUID productId, UUID warehouseId, String lotCode, LocalDate expiryDate,
		BigDecimal remainingQuantity) {

	public static ExpiringLotView from(Lot lot) {
		return new ExpiringLotView(lot.getProductId(), lot.getWarehouseId(), lot.getCode(), lot.getExpiryDate(),
				lot.getQuantity());
	}
}
