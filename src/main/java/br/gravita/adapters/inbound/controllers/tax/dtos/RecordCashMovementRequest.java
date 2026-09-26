package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.CashMovementType;
import br.gravita.core.ports.inbound.tax.RecordCashMovementCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RecordCashMovementRequest(@NotNull UUID sessionId, @NotNull CashMovementType type,
		@NotNull BigDecimal amount, @NotBlank String justification) {

	public RecordCashMovementCommand toCommand() {
		return new RecordCashMovementCommand(sessionId, type, amount, justification);
	}
}
