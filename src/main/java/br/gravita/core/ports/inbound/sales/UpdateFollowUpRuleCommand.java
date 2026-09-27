package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import lombok.Builder;

@Builder
public record UpdateFollowUpRuleCommand(FollowUpRuleId ruleId, Integer daysWithoutContact, FollowUpTarget target,
		Boolean notifyOwner, Boolean active) {
}
