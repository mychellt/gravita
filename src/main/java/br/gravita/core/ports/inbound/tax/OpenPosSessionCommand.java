package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record OpenPosSessionCommand(UUID registerId, UUID operatorId, BigDecimal openingChangeAmount) {

	public OpenPosSessionCommand {
		Objects.requireNonNull(registerId, "registerId is required");
		Objects.requireNonNull(operatorId, "operatorId is required");
		Objects.requireNonNull(openingChangeAmount, "openingChangeAmount is required");
	}
}
