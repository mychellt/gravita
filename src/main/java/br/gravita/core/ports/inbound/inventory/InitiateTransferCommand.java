package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
