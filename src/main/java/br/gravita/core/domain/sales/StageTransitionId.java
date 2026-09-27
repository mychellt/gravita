package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record StageTransitionId(UUID value) {

	public StageTransitionId {
		Objects.requireNonNull(value, "StageTransitionId value is required");
	}

	public static StageTransitionId of(UUID value) {
		return new StageTransitionId(value);
	}
}
