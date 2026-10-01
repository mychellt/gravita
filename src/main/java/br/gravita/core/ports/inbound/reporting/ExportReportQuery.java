package br.gravita.core.ports.inbound.reporting;

import br.gravita.core.domain.system.UserId;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code requesterId} is who exports, so the {@code EXPORT} permission on the report's screen can be checked against
 * their profile. The remaining fields are the target report's own query parameters, the same ones its screen takes;
 * a parameter the report does not use is ignored. {@code period} is the month of every report but the dashboard,
 * which takes {@code dashboardPeriod} (defaulting to the month) instead; {@code abcType} is required by the ABC
 * curve; {@code companyId} and {@code salesperson} only narrow the report when given.
 */
public record ExportReportQuery(UserId requesterId, ReportId reportId, ExportFormat format, YearMonth period,
		DashboardPeriod dashboardPeriod, AbcCurveType abcType, UUID companyId, UUID salesperson) {

	/** @throws IllegalArgumentException when a parameter the report requires is missing */
	public ExportReportQuery {
		Objects.requireNonNull(requesterId, "requesterId is required");
		Objects.requireNonNull(reportId, "reportId is required");
		Objects.requireNonNull(format, "format is required");
		if (reportId == ReportId.DASHBOARD) {
			dashboardPeriod = dashboardPeriod == null ? DashboardPeriod.MONTH : dashboardPeriod;
		} else if (period == null) {
			throw new IllegalArgumentException("period is required to export " + reportId.slug());
		}
		if (reportId == ReportId.ABC_CURVE && abcType == null) {
			throw new IllegalArgumentException("type is required to export " + reportId.slug());
		}
	}
}
