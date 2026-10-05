package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record PixChargeId(UUID value) {

	public PixChargeId {
		Objects.requireNonNull(value, "PixChargeId value is required");
	}

	public static PixChargeId of(final UUID value) {
		return new PixChargeId(value);
	}
}
