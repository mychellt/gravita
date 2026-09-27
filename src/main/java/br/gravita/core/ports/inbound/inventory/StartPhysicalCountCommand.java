package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCountScope;

import java.util.Objects;
import java.util.UUID;

public record StartPhysicalCountCommand(PhysicalCountScope scope, String productGroupId, UUID warehouseId,
		UUID user) {

	public StartPhysicalCountCommand {
		Objects.requireNonNull(scope, "scope is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(user, "user is required");
	}
}
