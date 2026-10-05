package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record StockReservationId(UUID value) {

	public StockReservationId {
		Objects.requireNonNull(value, "StockReservationId value is required");
	}

	public static StockReservationId of(final UUID value) {
		return new StockReservationId(value);
	}
}
