package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.domain.sales.FollowUpTask;
import java.time.LocalDate;
import java.util.UUID;

public record FollowUpTaskView(UUID id, UUID opportunityId, UUID customerId, LocalDate dueDate, UUID owner,
		AlertChannel alertChannel) {

	public static FollowUpTaskView from(FollowUpTask task) {
		return new FollowUpTaskView(
				task.getId().value(),
				task.getOpportunityId(),
				task.getCustomerId(),
				task.getDueDate(),
				task.getOwner(),
				task.getAlertChannel());
	}
}
