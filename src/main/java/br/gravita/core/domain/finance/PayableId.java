package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record PayableId(UUID value) {

	public PayableId {
		Objects.requireNonNull(value, "PayableId value is required");
	}

	public static PayableId of(UUID value) {
		return new PayableId(value);
	}
}
