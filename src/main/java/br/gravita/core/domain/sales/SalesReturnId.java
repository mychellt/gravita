package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record SalesReturnId(UUID value) {

	public SalesReturnId {
		Objects.requireNonNull(value, "SalesReturnId value is required");
	}

	public static SalesReturnId of(final UUID value) {
		return new SalesReturnId(value);
	}
}
