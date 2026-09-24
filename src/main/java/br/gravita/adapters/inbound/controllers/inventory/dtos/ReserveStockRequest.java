package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.ReserveStockCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record ReserveStockRequest(@NotNull UUID orderRef, @NotNull UUID productId, @NotNull UUID warehouseId,
		@NotNull @Positive BigDecimal quantity) {

	public ReserveStockCommand toCommand() {
		return new ReserveStockCommand(orderRef, productId, warehouseId, quantity);
	}
}
