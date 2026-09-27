package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.FollowUpRuleId;

public interface ManageFollowUpRuleUseCase {
	FollowUpRuleView create(CreateFollowUpRuleCommand command);

	FollowUpRuleView update(UpdateFollowUpRuleCommand command);

	void delete(FollowUpRuleId ruleId);
}
