package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.ports.inbound.sales.UpdateOpportunityCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateOpportunityRequest(
		UUID customerId,
		@PositiveOrZero BigDecimal estimatedValue,
		@Min(0) @Max(100) Integer probability,
		LocalDate expectedCloseDate,
		UUID owner) {

	public UpdateOpportunityCommand toCommand(UUID opportunityId) {
		return new UpdateOpportunityCommand(OpportunityId.of(opportunityId), customerId, estimatedValue, probability,
				expectedCloseDate, owner);
	}
}
