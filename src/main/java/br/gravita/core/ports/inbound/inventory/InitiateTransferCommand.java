package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code lotCode}/{@code serials} are stamped onto the resulting
 * {@code StockMovement} for traceability only - relocating a {@code Lot}'s or
 * {@code SerialUnit}'s own warehouse ownership across a transfer is out of
 * scope for UC-M5-05's acceptance criteria.
 */
public record InitiateTransferCommand(UUID productId, UUID sourceWarehouseId, UUID destinationWarehouseId,
		BigDecimal quantity, String lotCode, List<String> serials, UUID user) {

	public InitiateTransferCommand {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(sourceWarehouseId, "sourceWarehouseId is required");
		Objects.requireNonNull(destinationWarehouseId, "destinationWarehouseId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(user, "user is required");
		serials = serials == null ? List.of() : List.copyOf(serials);
	}
}
