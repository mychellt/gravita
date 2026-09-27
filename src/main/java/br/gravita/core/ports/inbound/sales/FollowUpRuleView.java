package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpTarget;
import java.util.UUID;

public record FollowUpRuleView(UUID id, int daysWithoutContact, FollowUpTarget target, boolean notifyOwner,
		boolean active) {

	public static FollowUpRuleView from(FollowUpRule rule) {
		return new FollowUpRuleView(rule.getId().value(), rule.getDaysWithoutContact(), rule.getTarget(),
				rule.isNotifyOwner(), rule.isActive());
	}
}
