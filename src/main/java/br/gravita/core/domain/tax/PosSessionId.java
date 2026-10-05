package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record PosSessionId(UUID value) {

	public PosSessionId {
		Objects.requireNonNull(value, "PosSessionId value is required");
	}

	public static PosSessionId of(final UUID value) {
		return new PosSessionId(value);
	}
}
