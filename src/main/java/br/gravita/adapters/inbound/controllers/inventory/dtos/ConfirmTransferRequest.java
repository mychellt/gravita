package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.ConfirmTransferCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmTransferRequest(@NotNull UUID user) {

	public ConfirmTransferCommand toCommand(UUID transferMovementId) {
		return new ConfirmTransferCommand(transferMovementId, user);
	}
}
