package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record FollowUpRuleId(UUID value) {

	public FollowUpRuleId {
		Objects.requireNonNull(value, "FollowUpRuleId value is required");
	}

	public static FollowUpRuleId of(final UUID value) {
		return new FollowUpRuleId(value);
	}
}
