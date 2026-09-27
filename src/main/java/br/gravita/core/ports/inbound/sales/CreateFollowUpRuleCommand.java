package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.FollowUpTarget;
import lombok.Builder;

@Builder
public record CreateFollowUpRuleCommand(int daysWithoutContact, FollowUpTarget target, boolean notifyOwner,
		boolean active) {
}
