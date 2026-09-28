package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CostCenterShare;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** {@code supplierId} and {@code costCenterSplit} are optional; {@code amount} and {@code dueDate} are required. */
public record CreateManualPayableCommand(UUID supplierId, BigDecimal amount, LocalDate dueDate,
		List<CostCenterShare> costCenterSplit) {

	public CreateManualPayableCommand {
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(dueDate, "dueDate is required");
	}
}
