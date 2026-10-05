package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.AlertChannel;
import br.gravita.core.ports.inbound.sales.FollowUpTaskView;
import java.time.LocalDate;
import java.util.UUID;

public record FollowUpTaskResponse(UUID id, UUID opportunityId, UUID customerId, LocalDate dueDate, UUID owner,
		AlertChannel alertChannel) {

	public static FollowUpTaskResponse from(final FollowUpTaskView view) {
		return new FollowUpTaskResponse(view.id(), view.opportunityId(), view.customerId(), view.dueDate(),
				view.owner(), view.alertChannel());
	}
}
