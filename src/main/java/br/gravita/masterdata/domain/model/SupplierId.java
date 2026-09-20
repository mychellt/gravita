package br.gravita.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SupplierId(UUID value) {

	public SupplierId {
		Objects.requireNonNull(value, "SupplierId value is required");
	}

	public static SupplierId of(UUID value) {
		return new SupplierId(value);
	}
}
