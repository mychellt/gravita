package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityNotFoundException;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.ports.inbound.sales.ChangeOpportunityStageCommand;
import br.gravita.core.ports.inbound.sales.CreateOpportunityCommand;
import br.gravita.core.ports.inbound.sales.ManageOpportunityUseCase;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import br.gravita.core.ports.inbound.sales.UpdateOpportunityCommand;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
import java.util.UUID;

@UseCase
public class ManageOpportunityService implements ManageOpportunityUseCase {

	private final OpportunityRepositoryPort opportunityRepositoryPort;
	private final StageTransitionRepositoryPort stageTransitionRepositoryPort;

	public ManageOpportunityService(OpportunityRepositoryPort opportunityRepositoryPort,
			StageTransitionRepositoryPort stageTransitionRepositoryPort) {
		this.opportunityRepositoryPort = opportunityRepositoryPort;
		this.stageTransitionRepositoryPort = stageTransitionRepositoryPort;
	}

	@Override
	public OpportunityView create(CreateOpportunityCommand command) {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		Opportunity opportunity = Opportunity.create(id, command.customerId(), command.estimatedValue(),
				command.probability(), command.expectedCloseDate(), command.owner());

		Opportunity saved = opportunityRepositoryPort.save(opportunity);
		return OpportunityView.from(saved);
	}

	@Override
	public OpportunityView update(UpdateOpportunityCommand command) {
		Opportunity existing = findOrThrow(command.opportunityId());

		Opportunity updated = existing.withUpdatedFields(command.customerId(), command.estimatedValue(),
				command.probability(), command.expectedCloseDate(), command.owner());

		Opportunity saved = opportunityRepositoryPort.save(updated);
		return OpportunityView.from(saved);
	}

	@Override
	public OpportunityView changeStage(ChangeOpportunityStageCommand command) {
		Opportunity existing = findOrThrow(command.opportunityId());

		Opportunity moved = existing.moveToStage(command.newStage());
		Opportunity saved = opportunityRepositoryPort.save(moved);

		StageTransition transition = StageTransition.append(existing.getId(), existing.getStage(),
				moved.getStage());
		stageTransitionRepositoryPort.save(transition);

		return OpportunityView.from(saved);
	}

	private Opportunity findOrThrow(OpportunityId opportunityId) {
		return opportunityRepositoryPort.findById(opportunityId)
				.orElseThrow(() -> new OpportunityNotFoundException(opportunityId.value()));
	}
}
