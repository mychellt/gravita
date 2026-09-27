package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.ports.inbound.sales.UpdateFollowUpRuleCommand;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UpdateFollowUpRuleRequest(
		@Positive Integer daysWithoutContact,
		FollowUpTarget target,
		Boolean notifyOwner,
		Boolean active) {

	public UpdateFollowUpRuleCommand toCommand(UUID ruleId) {
		return new UpdateFollowUpRuleCommand(FollowUpRuleId.of(ruleId), daysWithoutContact, target, notifyOwner,
				active);
	}
}
