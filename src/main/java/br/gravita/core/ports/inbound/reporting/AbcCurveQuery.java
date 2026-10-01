package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requesterId} is who opens the report, so its visibility can be checked against their profile.
 * {@code period} is the month whose invoiced revenue is classified. {@code companyId} narrows the revenue read
 * to one company; a {@code null} one does not restrict.
 */
public record AbcCurveQuery(UserId requesterId, AbcCurveType type, YearMonth period, UUID companyId) {

	public AbcCurveQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(type, "type is required");
		Objects.requireNonNull(period, "period is required");
	}
}
