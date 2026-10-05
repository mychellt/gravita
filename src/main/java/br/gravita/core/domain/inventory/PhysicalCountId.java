package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record PhysicalCountId(UUID value) {

	public PhysicalCountId {
		Objects.requireNonNull(value, "PhysicalCountId value is required");
	}

	public static PhysicalCountId of(final UUID value) {
		return new PhysicalCountId(value);
	}
}
