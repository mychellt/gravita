package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.RegisterStockExitCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegisterStockExitRequest(@NotNull UUID productId, @NotNull UUID warehouseId,
		@NotNull BigDecimal quantity, LotRequest lot, List<String> serials, UUID reservationId,
		Boolean allowNegativeStock, @NotBlank String originReference, @NotNull UUID user) {

	public RegisterStockExitCommand toCommand() {
		RegisterStockExitCommand.LotRef lotRef = lot == null ? null
				: new RegisterStockExitCommand.LotRef(lot.code());
		return new RegisterStockExitCommand(productId, warehouseId, quantity, lotRef, serials, reservationId,
				Boolean.TRUE.equals(allowNegativeStock), originReference, user);
	}

	public record LotRequest(@NotBlank String code) {
	}
}
