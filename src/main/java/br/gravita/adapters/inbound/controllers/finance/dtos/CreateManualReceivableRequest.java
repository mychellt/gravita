package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.ports.inbound.finance.CreateManualReceivableCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateManualReceivableRequest(
		@NotNull UUID customerId,
		@NotNull @Positive BigDecimal amount,
		@NotNull LocalDate dueDate,
		@Positive Integer installments) {

	public CreateManualReceivableCommand toCommand() {
		return new CreateManualReceivableCommand(customerId, amount, dueDate, installments);
	}
}
