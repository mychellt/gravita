package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record StockMovementId(UUID value) {

	public StockMovementId {
		Objects.requireNonNull(value, "StockMovementId value is required");
	}

	public static StockMovementId of(final UUID value) {
		return new StockMovementId(value);
	}
}
