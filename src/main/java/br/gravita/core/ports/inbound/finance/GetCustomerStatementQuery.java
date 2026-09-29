package br.gravita.core.ports.inbound.finance;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code from} and {@code to} are the optional period (both inclusive, each
 * may be {@code null} for no bound) the statement's lists are limited to.
 */
public record GetCustomerStatementQuery(UUID customerId, LocalDate from, LocalDate to) {

	public GetCustomerStatementQuery {
		Objects.requireNonNull(customerId, "customerId is required");
	}
}
