package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record InteractionId(UUID value) {

	public InteractionId {
		Objects.requireNonNull(value, "InteractionId value is required");
	}

	public static InteractionId of(UUID value) {
		return new InteractionId(value);
	}
}
