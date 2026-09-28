package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record BoletoId(UUID value) {

	public BoletoId {
		Objects.requireNonNull(value, "BoletoId value is required");
	}

	public static BoletoId of(UUID value) {
		return new BoletoId(value);
	}
}
