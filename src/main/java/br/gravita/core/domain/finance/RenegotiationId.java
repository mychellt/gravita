package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record RenegotiationId(UUID value) {

	public RenegotiationId {
		Objects.requireNonNull(value, "RenegotiationId value is required");
	}

	public static RenegotiationId of(UUID value) {
		return new RenegotiationId(value);
	}
}
