package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requesterId} is who opens the dashboard, so its visibility can be checked against their profile.
 * {@code companyId} narrows the sections whose source carries a company (receivables); a {@code null} one
 * does not restrict.
 */
public record DashboardQuery(UserId requesterId, DashboardPeriod period, UUID companyId) {

	public DashboardQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
