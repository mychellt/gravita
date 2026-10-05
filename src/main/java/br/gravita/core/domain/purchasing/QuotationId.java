package br.gravita.core.domain.purchasing;

import java.util.Objects;
import java.util.UUID;

public record QuotationId(UUID value) {

	public QuotationId {
		Objects.requireNonNull(value, "QuotationId value is required");
	}

	public static QuotationId of(final UUID value) {
		return new QuotationId(value);
	}
}
