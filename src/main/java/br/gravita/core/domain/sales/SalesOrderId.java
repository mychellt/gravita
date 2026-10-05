package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record SalesOrderId(UUID value) {

	public SalesOrderId {
		Objects.requireNonNull(value, "SalesOrderId value is required");
	}

	public static SalesOrderId of(final UUID value) {
		return new SalesOrderId(value);
	}
}
