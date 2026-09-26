package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.CashMovementType;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record RecordCashMovementCommand(UUID sessionId, CashMovementType type, BigDecimal amount,
		String justification) {

	public RecordCashMovementCommand {
		Objects.requireNonNull(sessionId, "sessionId is required");
		Objects.requireNonNull(type, "type is required");
		Objects.requireNonNull(amount, "amount is required");
	}
}
