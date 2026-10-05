package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.ports.inbound.sales.FollowUpRuleView;
import java.util.UUID;

public record FollowUpRuleResponse(UUID id, int daysWithoutContact, FollowUpTarget target, boolean notifyOwner,
		boolean active) {

	public static FollowUpRuleResponse from(final FollowUpRuleView view) {
		return new FollowUpRuleResponse(view.id(), view.daysWithoutContact(), view.target(), view.notifyOwner(),
				view.active());
	}
}
