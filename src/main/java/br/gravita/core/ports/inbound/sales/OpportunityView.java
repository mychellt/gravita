package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityStage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityView(UUID id, UUID customerId, BigDecimal estimatedValue, Integer probability,
		LocalDate expectedCloseDate, UUID owner, OpportunityStage stage) {

	public static OpportunityView from(final Opportunity opportunity) {
		return new OpportunityView(
				opportunity.getId().value(),
				opportunity.getCustomerId(),
				opportunity.getEstimatedValue(),
				opportunity.getProbability(),
				opportunity.getExpectedCloseDate(),
				opportunity.getOwner(),
				opportunity.getStage());
	}
}
