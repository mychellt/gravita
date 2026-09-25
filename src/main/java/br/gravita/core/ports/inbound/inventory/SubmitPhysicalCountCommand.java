package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCountId;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record SubmitPhysicalCountCommand(PhysicalCountId physicalCountId, Map<UUID, BigDecimal> countedQuantities,
		UUID submittedBy) {

	public SubmitPhysicalCountCommand {
		Objects.requireNonNull(physicalCountId, "physicalCountId is required");
		Objects.requireNonNull(countedQuantities, "countedQuantities is required");
		Objects.requireNonNull(submittedBy, "submittedBy is required");
	}
}
