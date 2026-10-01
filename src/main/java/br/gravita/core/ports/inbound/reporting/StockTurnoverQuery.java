package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requesterId} is who opens the report, so its visibility can be checked against their profile.
 * {@code period} is the month the turnover is measured over. {@code companyId} is passed through to the inventory
 * read; a {@code null} one does not restrict.
 */
public record StockTurnoverQuery(UserId requesterId, YearMonth period, UUID companyId) {

	public StockTurnoverQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
