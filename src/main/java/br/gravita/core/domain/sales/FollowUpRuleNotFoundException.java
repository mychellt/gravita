package br.gravita.core.domain.sales;

import java.util.UUID;

public class FollowUpRuleNotFoundException extends RuntimeException {

	public FollowUpRuleNotFoundException(UUID ruleId) {
		super("Follow-up rule not found: " + ruleId);
	}
}
