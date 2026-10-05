package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record InboundNfeId(UUID value) {

	public InboundNfeId {
		Objects.requireNonNull(value, "InboundNfeId value is required");
	}

	public static InboundNfeId of(final UUID value) {
		return new InboundNfeId(value);
	}
}
