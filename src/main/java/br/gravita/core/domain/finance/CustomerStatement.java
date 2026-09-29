package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read model: everything a customer has been billed, has paid and has
 * renegotiated, each list in chronological order, plus what they still owe.
 * {@code from} and {@code to} (both inclusive, {@code null} when open) bound
 * the lists - titles by due date, settlements by the day they were made and
 * renegotiations by the day they were agreed - but never {@code openBalance},
 * which is always the customer's current position. Immutable.
 */
public record CustomerStatement(UUID customerId, LocalDate from, LocalDate to, List<Receivable> titles,
		List<Settlement> settlements, List<Renegotiation> renegotiations, BigDecimal openBalance) {

	public CustomerStatement {
		Objects.requireNonNull(customerId, "customerId is required");
		titles = List.copyOf(titles);
		settlements = List.copyOf(settlements);
		renegotiations = List.copyOf(renegotiations);
		Objects.requireNonNull(openBalance, "openBalance is required");
	}
}
