package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.AdjustInventoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AdjustInventoryRequest(@NotNull UUID productId, @NotNull UUID warehouseId,
		@NotNull BigDecimal quantityDelta, @NotBlank String justification, @NotNull UUID user) {

	public AdjustInventoryCommand toCommand() {
		return new AdjustInventoryCommand(productId, warehouseId, quantityDelta, justification, user);
	}
}
