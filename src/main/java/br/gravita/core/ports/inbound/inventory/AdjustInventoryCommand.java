package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AdjustInventoryCommand(UUID productId, UUID warehouseId, BigDecimal quantityDelta, String justification,
		UUID user) {

	public AdjustInventoryCommand {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(quantityDelta, "quantityDelta is required");
		Objects.requireNonNull(user, "user is required");
	}
}
