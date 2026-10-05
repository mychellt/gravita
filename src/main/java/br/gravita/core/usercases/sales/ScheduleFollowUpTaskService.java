package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.FollowUpTaskId;
import br.gravita.core.ports.inbound.sales.FollowUpTaskView;
import br.gravita.core.ports.inbound.sales.ScheduleFollowUpTaskCommand;
import br.gravita.core.ports.inbound.sales.ScheduleFollowUpTaskUseCase;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpTaskRepositoryPort;
import br.gravita.core.ports.outbound.sales.SendFollowUpAlertPort;
import java.util.UUID;

@UseCase
public class ScheduleFollowUpTaskService implements ScheduleFollowUpTaskUseCase {

	private final FollowUpTaskRepositoryPort followUpTaskRepositoryPort;
	private final SendFollowUpAlertPort sendFollowUpAlertPort;

	public ScheduleFollowUpTaskService(final FollowUpTaskRepositoryPort followUpTaskRepositoryPort,
			final SendFollowUpAlertPort sendFollowUpAlertPort) {
		this.followUpTaskRepositoryPort = followUpTaskRepositoryPort;
		this.sendFollowUpAlertPort = sendFollowUpAlertPort;
	}

	@Override
	public FollowUpTaskView execute(final ScheduleFollowUpTaskCommand command) {
		final FollowUpTaskId id = FollowUpTaskId.of(UUID.randomUUID());
		final FollowUpTask task = FollowUpTask.schedule(id, command.opportunityId(), command.customerId(),
				command.dueDate(), command.owner(), command.alertChannel());

		final FollowUpTask saved = followUpTaskRepositoryPort.save(task);
		sendFollowUpAlertPort.send(saved);

		return FollowUpTaskView.from(saved);
	}
}
