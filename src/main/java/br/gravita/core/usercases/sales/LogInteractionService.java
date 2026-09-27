package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.ports.inbound.sales.InteractionView;
import br.gravita.core.ports.inbound.sales.LogInteractionCommand;
import br.gravita.core.ports.inbound.sales.LogInteractionUseCase;
import br.gravita.core.ports.outbound.persistence.sales.InteractionRepositoryPort;

@UseCase
public class LogInteractionService implements LogInteractionUseCase {

	private final InteractionRepositoryPort interactionRepositoryPort;

	public LogInteractionService(InteractionRepositoryPort interactionRepositoryPort) {
		this.interactionRepositoryPort = interactionRepositoryPort;
	}

	@Override
	public InteractionView execute(LogInteractionCommand command) {
		Interaction interaction = Interaction.log(command.opportunityId(), command.customerId(), command.channel(),
				command.summary(), command.timestamp());

		Interaction saved = interactionRepositoryPort.save(interaction);
		return InteractionView.from(saved);
	}
}
