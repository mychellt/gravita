package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.ports.inbound.sales.CreateFollowUpRuleCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateFollowUpRuleRequest(
		@NotNull @Positive Integer daysWithoutContact,
		@NotNull FollowUpTarget target,
		Boolean notifyOwner,
		Boolean active) {

	public CreateFollowUpRuleCommand toCommand() {
		return new CreateFollowUpRuleCommand(daysWithoutContact, target, Boolean.TRUE.equals(notifyOwner),
				active == null || active);
	}
}
