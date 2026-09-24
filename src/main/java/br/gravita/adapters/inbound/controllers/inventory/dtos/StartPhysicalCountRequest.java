package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartPhysicalCountRequest(@NotNull PhysicalCountScope scope, String productGroupId,
		@NotNull UUID warehouseId, @NotNull UUID user) {

	public StartPhysicalCountCommand toCommand() {
		return new StartPhysicalCountCommand(scope, productGroupId, warehouseId, user);
	}
}
