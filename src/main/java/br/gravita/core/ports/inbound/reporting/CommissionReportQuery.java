package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requesterId} is who opens the report, so its visibility can be checked against their profile.
 * {@code period} is the month whose invoiced orders' commissions are listed. {@code salesperson} narrows the
 * report to one salesperson; a {@code null} one lists everybody.
 */
public record CommissionReportQuery(UserId requesterId, UUID salesperson, YearMonth period) {

	public CommissionReportQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
