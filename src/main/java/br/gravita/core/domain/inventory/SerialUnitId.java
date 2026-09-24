package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record SerialUnitId(UUID value) {

	public SerialUnitId {
		Objects.requireNonNull(value, "SerialUnitId value is required");
	}

	public static SerialUnitId of(UUID value) {
		return new SerialUnitId(value);
	}
}
