package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCountId;

import java.util.Objects;
import java.util.UUID;

public record ApprovePhysicalCountCommand(PhysicalCountId physicalCountId, UUID approvedBy) {

	public ApprovePhysicalCountCommand {
		Objects.requireNonNull(physicalCountId, "physicalCountId is required");
		Objects.requireNonNull(approvedBy, "approvedBy is required");
	}
}
