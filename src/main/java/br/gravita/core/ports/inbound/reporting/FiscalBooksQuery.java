package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;

/**
 * {@code requesterId} is who opens the books, so their visibility can be checked against their profile.
 * {@code period} is the month whose fiscal documents are booked.
 */
public record FiscalBooksQuery(UserId requesterId, YearMonth period) {

	public FiscalBooksQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
