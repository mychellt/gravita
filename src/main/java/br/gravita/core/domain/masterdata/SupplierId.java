package br.gravita.core.domain.masterdata;

import java.util.Objects;
import java.util.UUID;

public record SupplierId(UUID value) {

	public SupplierId {
		Objects.requireNonNull(value, "SupplierId value is required");
	}

	public static SupplierId of(final UUID value) {
		return new SupplierId(value);
	}
}
