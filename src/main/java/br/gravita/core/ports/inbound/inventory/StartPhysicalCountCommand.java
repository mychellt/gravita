package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCountScope;

import java.util.Objects;
import java.util.UUID;

/**
 * {@code productGroupId} corresponds to {@code ProductDomain.classification().group()}
 * (M1 doesn't model product groups as a separate id-bearing entity yet); it
 * is required when {@code scope} is {@code PARTIAL_BY_GROUP} and ignored
 * otherwise - the domain enforces that consistency (UC-M5-08, AC1).
 */
public record StartPhysicalCountCommand(PhysicalCountScope scope, String productGroupId, UUID warehouseId,
		UUID user) {

	public StartPhysicalCountCommand {
		Objects.requireNonNull(scope, "scope is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(user, "user is required");
	}
}
