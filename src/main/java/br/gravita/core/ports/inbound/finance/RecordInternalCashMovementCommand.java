package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CashMovementDirection;
import java.math.BigDecimal;
import java.util.Objects;

/** All three fields are required; {@code justification} must not be blank. */
public record RecordInternalCashMovementCommand(CashMovementDirection direction, BigDecimal amount,
		String justification) {

	public RecordInternalCashMovementCommand {
		Objects.requireNonNull(direction, "direction is required");
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(justification, "justification is required");
	}
}
