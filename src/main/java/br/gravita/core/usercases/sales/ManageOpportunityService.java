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

	public ManageOpportunityService(final OpportunityRepositoryPort opportunityRepositoryPort,
			final StageTransitionRepositoryPort stageTransitionRepositoryPort) {
		this.opportunityRepositoryPort = opportunityRepositoryPort;
		this.stageTransitionRepositoryPort = stageTransitionRepositoryPort;
	}

	@Override
	public OpportunityView create(final CreateOpportunityCommand command) {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		final Opportunity opportunity = Opportunity.create(id, command.customerId(), command.estimatedValue(),
				command.probability(), command.expectedCloseDate(), command.owner());

		final Opportunity saved = opportunityRepositoryPort.save(opportunity);
		return OpportunityView.from(saved);
	}

	@Override
	public OpportunityView update(final UpdateOpportunityCommand command) {
		final Opportunity existing = findOrThrow(command.opportunityId());

		final Opportunity updated = existing.withUpdatedFields(command.customerId(), command.estimatedValue(),
				command.probability(), command.expectedCloseDate(), command.owner());

		final Opportunity saved = opportunityRepositoryPort.save(updated);
		return OpportunityView.from(saved);
	}

	@Override
	public OpportunityView changeStage(final ChangeOpportunityStageCommand command) {
		final Opportunity existing = findOrThrow(command.opportunityId());

		final Opportunity moved = existing.moveToStage(command.newStage());
		final Opportunity saved = opportunityRepositoryPort.save(moved);

		final StageTransition transition = StageTransition.append(existing.getId(), existing.getStage(),
				moved.getStage());
		stageTransitionRepositoryPort.save(transition);

		return OpportunityView.from(saved);
	}

	private Opportunity findOrThrow(final OpportunityId opportunityId) {
		return opportunityRepositoryPort.findById(opportunityId)
				.orElseThrow(() -> new OpportunityNotFoundException(opportunityId.value()));
	}
}
