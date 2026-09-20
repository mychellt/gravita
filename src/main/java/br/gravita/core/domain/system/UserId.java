package br.gravita.core.domain.system;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

	public UserId {
		Objects.requireNonNull(value, "UserId value is required");
	}

	public static UserId of(UUID value) {
		return new UserId(value);
	}

	public static UserId generate() {
		return new UserId(UUID.randomUUID());
	}
}
