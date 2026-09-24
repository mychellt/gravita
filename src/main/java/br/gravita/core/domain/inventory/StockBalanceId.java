package br.gravita.core.domain.inventory;

import java.util.Objects;
import java.util.UUID;

public record StockBalanceId(UUID value) {

	public StockBalanceId {
		Objects.requireNonNull(value, "StockBalanceId value is required");
	}

	public static StockBalanceId of(UUID value) {
		return new StockBalanceId(value);
	}
}
