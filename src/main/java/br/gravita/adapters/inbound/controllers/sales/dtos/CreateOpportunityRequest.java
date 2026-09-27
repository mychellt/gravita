package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.CreateOpportunityCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOpportunityRequest(
		@NotNull UUID customerId,
		@NotNull @PositiveOrZero BigDecimal estimatedValue,
		@NotNull @Min(0) @Max(100) Integer probability,
		@NotNull LocalDate expectedCloseDate,
		@NotNull UUID owner) {

	public CreateOpportunityCommand toCommand() {
		return new CreateOpportunityCommand(customerId, estimatedValue, probability, expectedCloseDate, owner);
	}
}
