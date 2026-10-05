package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RegisterStockEntryRequest(@NotNull UUID productId, @NotNull UUID warehouseId,
		@NotNull BigDecimal quantity, @NotNull BigDecimal unitCost, LotRequest lot, List<String> serials,
		@NotBlank String originReference, @NotNull UUID user) {

	public RegisterStockEntryCommand toCommand() {
		final RegisterStockEntryCommand.LotDetails lotDetails = lot == null ? null
				: new RegisterStockEntryCommand.LotDetails(lot.code(), lot.expiryDate());
		return new RegisterStockEntryCommand(productId, warehouseId, quantity, unitCost, lotDetails, serials,
				originReference, user);
	}

	public record LotRequest(@NotBlank String code, LocalDate expiryDate) {
	}
}
