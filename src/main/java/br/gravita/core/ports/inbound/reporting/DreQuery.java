package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requesterId} is who opens the report, so its visibility can be checked against their profile.
 * {@code period} is the month the DRE covers. {@code costCenter} narrows the expenses to that cost center; a
 * {@code null} one reports every cost center. {@code companyId} is passed through to the reads that can be narrowed
 * by it; a {@code null} one does not restrict.
 */
public record DreQuery(UserId requesterId, YearMonth period, UUID costCenter, UUID companyId) {

	public DreQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
