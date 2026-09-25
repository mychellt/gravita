package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record StockTransferId(UUID value) {

	public StockTransferId {
		Objects.requireNonNull(value, "StockTransferId value is required");
	}

	public static StockTransferId of(UUID value) {
		return new StockTransferId(value);
	}
}
