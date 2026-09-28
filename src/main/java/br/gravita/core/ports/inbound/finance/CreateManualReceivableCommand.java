package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CreateManualReceivableCommand(UUID customerId, BigDecimal amount, LocalDate dueDate,
		Integer installments) {

	public CreateManualReceivableCommand {
		Objects.requireNonNull(customerId, "customerId is required");
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(dueDate, "dueDate is required");
	}
}
