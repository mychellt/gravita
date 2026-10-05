package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record LotId(UUID value) {

	public LotId {
		Objects.requireNonNull(value, "LotId value is required");
	}

	public static LotId of(final UUID value) {
		return new LotId(value);
	}
}
