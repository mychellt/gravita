package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record CashMovementId(UUID value) {

	public CashMovementId {
		Objects.requireNonNull(value, "CashMovementId value is required");
	}

	public static CashMovementId of(final UUID value) {
		return new CashMovementId(value);
	}
}
