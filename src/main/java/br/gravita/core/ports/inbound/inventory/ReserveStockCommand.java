package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record ReserveStockCommand(UUID orderRef, UUID productId, UUID warehouseId, BigDecimal quantity) {

	public ReserveStockCommand {
		Objects.requireNonNull(orderRef, "orderRef is required");
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(quantity, "quantity is required");
	}
}
