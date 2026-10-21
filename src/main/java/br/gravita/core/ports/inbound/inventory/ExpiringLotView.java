package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.Lot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpiringLotView(UUID productId, UUID warehouseId, String lotCode, LocalDate expiryDate,
		BigDecimal remainingQuantity, String productCode) {

	public ExpiringLotView(final UUID productId, final UUID warehouseId, final String lotCode, final LocalDate expiryDate,
			final BigDecimal remainingQuantity) {
		this(productId, warehouseId, lotCode, expiryDate, remainingQuantity, null);
	}

	public ExpiringLotView withProductCode(final String code) {
		return new ExpiringLotView(productId, warehouseId, lotCode, expiryDate, remainingQuantity, code);
	}

	public static ExpiringLotView from(final Lot lot) {
		return new ExpiringLotView(lot.getProductId(), lot.getWarehouseId(), lot.getCode(), lot.getExpiryDate(),
				lot.getQuantity());
	}
}
