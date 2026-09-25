package br.gravita.core.ports.inbound.inventory;

import java.util.Objects;
import java.util.UUID;

public record ConfirmTransferCommand(UUID transferMovementId, UUID user) {

	public ConfirmTransferCommand {
		Objects.requireNonNull(transferMovementId, "transferMovementId is required");
		Objects.requireNonNull(user, "user is required");
	}
}
