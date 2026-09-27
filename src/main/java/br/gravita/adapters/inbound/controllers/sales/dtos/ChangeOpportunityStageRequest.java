package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.ports.inbound.sales.ChangeOpportunityStageCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeOpportunityStageRequest(@NotNull OpportunityStage newStage) {

	public ChangeOpportunityStageCommand toCommand(UUID opportunityId) {
		return new ChangeOpportunityStageCommand(OpportunityId.of(opportunityId), newStage);
	}
}
