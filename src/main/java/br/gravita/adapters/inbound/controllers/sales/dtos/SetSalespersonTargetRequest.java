package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.SetSalespersonTargetCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record SetSalespersonTargetRequest(@NotNull @PositiveOrZero BigDecimal valueTarget,
		@NotNull @PositiveOrZero Integer orderCountTarget) {

	public SetSalespersonTargetCommand toCommand(UUID salesperson, YearMonth month) {
		return new SetSalespersonTargetCommand(salesperson, month, valueTarget, orderCountTarget);
	}
}
