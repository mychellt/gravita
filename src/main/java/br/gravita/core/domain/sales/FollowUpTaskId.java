package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record FollowUpTaskId(UUID value) {

	public FollowUpTaskId {
		Objects.requireNonNull(value, "FollowUpTaskId value is required");
	}

	public static FollowUpTaskId of(final UUID value) {
		return new FollowUpTaskId(value);
	}
}
