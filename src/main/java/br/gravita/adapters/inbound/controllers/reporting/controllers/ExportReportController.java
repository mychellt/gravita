package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.AbcCurveType;
import br.gravita.core.ports.inbound.reporting.DashboardPeriod;
import br.gravita.core.ports.inbound.reporting.ExportFormat;
import br.gravita.core.ports.inbound.reporting.ExportReportQuery;
import br.gravita.core.ports.inbound.reporting.ExportReportUseCase;
import br.gravita.core.ports.inbound.reporting.ExportedFile;
import br.gravita.core.ports.inbound.reporting.ReportId;
import java.time.YearMonth;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports/{reportId}/export")
public class ExportReportController {

	private final ExportReportUseCase exportReportUseCase;

	/**
	 * {@code format} is {@code pdf} or {@code xlsx}. The other parameters are the target report's own, as on its
	 * screen: {@code period} ({@code yyyy-MM}) for every report but the dashboard, which takes {@code dashboardPeriod}
	 * ({@code DAY}, {@code WEEK} or {@code MONTH}, default {@code MONTH}); {@code type} ({@code product} or
	 * {@code customer}) for the ABC curve; and the optional {@code companyId} and {@code salesperson} filters.
	 */
	@GetMapping
	public ResponseEntity<byte[]> export(@AuthenticatedUser final UserId callerId, @PathVariable final String reportId,
			@RequestParam final String format,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") final YearMonth period,
			@RequestParam(required = false) final DashboardPeriod dashboardPeriod,
			@RequestParam(required = false) final String type, @RequestParam(required = false) final UUID companyId,
			@RequestParam(required = false) final UUID salesperson) {
		final ReportId report = ReportId.fromSlug(reportId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report " + reportId));
		final ExportFormat exportFormat = ExportFormat.fromExtension(format)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "format must be pdf or xlsx"));
		final ExportedFile file = exportReportUseCase.execute(query(callerId, report, exportFormat, period, dashboardPeriod,
				type, companyId, salesperson));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.filename()).build().toString())
				.contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
	}

	private ExportReportQuery query(final UserId callerId, final ReportId report, final ExportFormat format, final YearMonth period,
			final DashboardPeriod dashboardPeriod, final String type, final UUID companyId, final UUID salesperson) {
		try {
			return new ExportReportQuery(callerId, report, format, period, dashboardPeriod, abcType(type), companyId,
					salesperson);
		} catch (final IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	private AbcCurveType abcType(final String type) {
		if (type == null) {
			return null;
		}
		try {
			return AbcCurveType.valueOf(type.trim().toUpperCase(Locale.ROOT));
		} catch (final IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be product or customer");
		}
	}
}
