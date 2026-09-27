package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import lombok.Builder;

@Builder
public record ChangeOpportunityStageCommand(OpportunityId opportunityId, OpportunityStage newStage) {
}
