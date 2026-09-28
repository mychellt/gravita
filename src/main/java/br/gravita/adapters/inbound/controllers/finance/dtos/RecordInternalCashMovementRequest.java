package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RecordInternalCashMovementRequest(
		@NotNull CashMovementDirection direction,
		@NotNull @Positive BigDecimal amount,
		@NotBlank @Size(max = 500) String justification) {

	public RecordInternalCashMovementCommand toCommand() {
		return new RecordInternalCashMovementCommand(direction, amount, justification);
	}
}
