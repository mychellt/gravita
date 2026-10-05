package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record SalesInvoiceId(UUID value) {

	public SalesInvoiceId {
		Objects.requireNonNull(value, "SalesInvoiceId value is required");
	}

	public static SalesInvoiceId of(final UUID value) {
		return new SalesInvoiceId(value);
	}
}
