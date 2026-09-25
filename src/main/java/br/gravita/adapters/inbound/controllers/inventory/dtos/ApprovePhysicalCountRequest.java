package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.ports.inbound.inventory.ApprovePhysicalCountCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApprovePhysicalCountRequest(@NotNull UUID approvedBy) {

	public ApprovePhysicalCountCommand toCommand(UUID physicalCountId) {
		return new ApprovePhysicalCountCommand(PhysicalCountId.of(physicalCountId), approvedBy);
	}
}
