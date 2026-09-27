package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityResponse(UUID id, UUID customerId, BigDecimal estimatedValue, Integer probability,
		LocalDate expectedCloseDate, UUID owner, OpportunityStage stage) {

	public static OpportunityResponse from(OpportunityView view) {
		return new OpportunityResponse(view.id(), view.customerId(), view.estimatedValue(), view.probability(),
				view.expectedCloseDate(), view.owner(), view.stage());
	}
}
