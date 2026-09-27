package br.gravita.core.ports.inbound.sales;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public record SetSalespersonTargetCommand(UUID salesperson, YearMonth month, BigDecimal valueTarget,
		int orderCountTarget) {

	public SetSalespersonTargetCommand {
		Objects.requireNonNull(salesperson, "salesperson is required");
		Objects.requireNonNull(month, "month is required");
	}
}
