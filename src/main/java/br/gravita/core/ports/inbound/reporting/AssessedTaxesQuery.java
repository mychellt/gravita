package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;

/**
 * {@code requesterId} is who asks for the summary, so their visibility can be checked against their profile.
 * {@code period} is the month whose authorized fiscal documents are summed.
 */
public record AssessedTaxesQuery(UserId requesterId, YearMonth period) {

	public AssessedTaxesQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
