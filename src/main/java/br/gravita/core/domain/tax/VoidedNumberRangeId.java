package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record VoidedNumberRangeId(UUID value) {

	public VoidedNumberRangeId {
		Objects.requireNonNull(value, "VoidedNumberRangeId value is required");
	}

	public static VoidedNumberRangeId of(UUID value) {
		return new VoidedNumberRangeId(value);
	}
}
