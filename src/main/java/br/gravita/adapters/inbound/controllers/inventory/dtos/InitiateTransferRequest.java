package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.InitiateTransferCommand;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InitiateTransferRequest(@NotNull UUID productId, @NotNull UUID sourceWarehouseId,
		@NotNull UUID destinationWarehouseId, @NotNull BigDecimal quantity, String lotCode, List<String> serials,
		@NotNull UUID user) {

	public InitiateTransferCommand toCommand() {
		return new InitiateTransferCommand(productId, sourceWarehouseId, destinationWarehouseId, quantity, lotCode,
				serials, user);
	}
}
