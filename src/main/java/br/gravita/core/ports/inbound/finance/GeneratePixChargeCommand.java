package br.gravita.core.ports.inbound.finance;

import java.util.Objects;
import java.util.UUID;

public record GeneratePixChargeCommand(UUID receivableId) {

	public GeneratePixChargeCommand {
		Objects.requireNonNull(receivableId, "receivableId is required");
	}
}
