package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record QuoteId(UUID value) {

	public QuoteId {
		Objects.requireNonNull(value, "QuoteId value is required");
	}

	public static QuoteId of(final UUID value) {
		return new QuoteId(value);
	}
}
