package br.gravita.core.ports.inbound.sales;

public interface ManageOpportunityUseCase {
	OpportunityView create(CreateOpportunityCommand command);

	OpportunityView update(UpdateOpportunityCommand command);

	OpportunityView changeStage(ChangeOpportunityStageCommand command);
}
