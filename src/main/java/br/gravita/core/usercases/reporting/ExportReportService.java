package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.inbound.reporting.AbcCurveQuery;
import br.gravita.core.ports.inbound.reporting.AssessedTaxesQuery;
import br.gravita.core.ports.inbound.reporting.CommissionReportQuery;
import br.gravita.core.ports.inbound.reporting.DashboardQuery;
import br.gravita.core.ports.inbound.reporting.ExportFormat;
import br.gravita.core.ports.inbound.reporting.ExportReportQuery;
import br.gravita.core.ports.inbound.reporting.ExportReportUseCase;
import br.gravita.core.ports.inbound.reporting.ExportedFile;
import br.gravita.core.ports.inbound.reporting.FiscalBooks;
import br.gravita.core.ports.inbound.reporting.FiscalBooksQuery;
import br.gravita.core.ports.inbound.reporting.GetAbcCurveUseCase;
import br.gravita.core.ports.inbound.reporting.GetAssessedTaxesUseCase;
import br.gravita.core.ports.inbound.reporting.GetCommissionReportUseCase;
import br.gravita.core.ports.inbound.reporting.GetExecutiveDashboardUseCase;
import br.gravita.core.ports.inbound.reporting.GetFiscalBooksUseCase;
import br.gravita.core.ports.inbound.reporting.GetStockTurnoverUseCase;
import br.gravita.core.ports.inbound.reporting.ReportId;
import br.gravita.core.ports.inbound.reporting.StockTurnoverQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.RenderExcelPort;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Exports any report of the module as a PDF or an Excel file. It runs the report's own use case, so what is exported
 * is what the screen shows for the same parameters, under the same visibility rule (and the dashboard's short-lived
 * cache); on top of that the user's profile must grant {@code EXPORT} on the report's screen, checked before the
 * report is read. The result is laid out once by {@link ExportLayouts} and handed to the renderer of the format, so
 * a report costs one layout, not one per format. The Entries, Exits and ICMS Assessment books are the exception for
 * PDF: {@link FiscalBooks#pdf()} is already the statutory rendering, so it is returned as is.
 * <p>
 * The DRE and the supplier purchase summary have no report to run yet, so exporting them is refused as not found.
 */
@UseCase
public class ExportReportService implements ExportReportUseCase {

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

	private final GetExecutiveDashboardUseCase getExecutiveDashboardUseCase;
	private final GetAbcCurveUseCase getAbcCurveUseCase;
	private final GetStockTurnoverUseCase getStockTurnoverUseCase;
	private final GetCommissionReportUseCase getCommissionReportUseCase;
	private final GetFiscalBooksUseCase getFiscalBooksUseCase;
	private final GetAssessedTaxesUseCase getAssessedTaxesUseCase;
	private final RenderPdfPort renderPdfPort;
	private final RenderExcelPort renderExcelPort;
	private final PermissionCheckPort permissionCheckPort;

	public ExportReportService(GetExecutiveDashboardUseCase getExecutiveDashboardUseCase,
			GetAbcCurveUseCase getAbcCurveUseCase, GetStockTurnoverUseCase getStockTurnoverUseCase,
			GetCommissionReportUseCase getCommissionReportUseCase, GetFiscalBooksUseCase getFiscalBooksUseCase,
			GetAssessedTaxesUseCase getAssessedTaxesUseCase, RenderPdfPort renderPdfPort,
			RenderExcelPort renderExcelPort, PermissionCheckPort permissionCheckPort) {
		this.getExecutiveDashboardUseCase = getExecutiveDashboardUseCase;
		this.getAbcCurveUseCase = getAbcCurveUseCase;
		this.getStockTurnoverUseCase = getStockTurnoverUseCase;
		this.getCommissionReportUseCase = getCommissionReportUseCase;
		this.getFiscalBooksUseCase = getFiscalBooksUseCase;
		this.getAssessedTaxesUseCase = getAssessedTaxesUseCase;
		this.renderPdfPort = renderPdfPort;
		this.renderExcelPort = renderExcelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public ExportedFile execute(ExportReportQuery query) {
		ReportId report = query.reportId();
		if (!permissionCheckPort.canExport(query.requesterId(), report.slug())) {
			throw new ForbiddenException("The user's profile cannot export the " + report.slug() + " report");
		}
		return switch (report) {
			case DASHBOARD -> render(query, "dashboard-" + query.dashboardPeriod().name().toLowerCase(Locale.ROOT),
					ExportLayouts.dashboard(getExecutiveDashboardUseCase
							.execute(new DashboardQuery(query.requesterId(), query.dashboardPeriod(), query.companyId()))));
			case ABC_CURVE -> render(query,
					"abc-curve-" + query.abcType().name().toLowerCase(Locale.ROOT) + "-" + query.period().format(MONTH),
					ExportLayouts.abcCurve(query.abcType(), query.period(), getAbcCurveUseCase.execute(
							new AbcCurveQuery(query.requesterId(), query.abcType(), query.period(), query.companyId()))));
			case STOCK_TURNOVER -> render(query, "stock-turnover-" + query.period().format(MONTH),
					ExportLayouts.stockTurnover(query.period(), getStockTurnoverUseCase.execute(
							new StockTurnoverQuery(query.requesterId(), query.period(), query.companyId()))));
			case COMMISSIONS -> render(query, "commissions-" + query.period().format(MONTH),
					ExportLayouts.commissions(query.period(), getCommissionReportUseCase.execute(
							new CommissionReportQuery(query.requesterId(), query.salesperson(), query.period()))));
			case FISCAL_BOOKS -> fiscalBooks(query);
			case ASSESSED_TAXES -> render(query, "assessed-taxes-" + query.period().format(MONTH),
					ExportLayouts.assessedTaxes(getAssessedTaxesUseCase
							.execute(new AssessedTaxesQuery(query.requesterId(), query.period()))));
			case DRE, PURCHASES_BY_SUPPLIER -> throw new ResourceNotFoundException(
					"The " + report.slug() + " report is not available to export yet");
		};
	}

	private ExportedFile fiscalBooks(ExportReportQuery query) {
		FiscalBooks books = getFiscalBooksUseCase.execute(new FiscalBooksQuery(query.requesterId(), query.period()));
		String name = "fiscal-books-" + query.period().format(MONTH);
		if (query.format() == ExportFormat.PDF) {
			return file(query.format(), name, books.pdf());
		}
		return render(query, name, ExportLayouts.fiscalBooks(books));
	}

	private ExportedFile render(ExportReportQuery query, String name, ExportLayout layout) {
		byte[] content = switch (query.format()) {
			case PDF -> renderPdfPort.render(layout.pdfReport());
			case XLSX -> renderExcelPort.render(layout.excelWorkbook());
		};
		return file(query.format(), name, content);
	}

	private static ExportedFile file(ExportFormat format, String name, byte[] content) {
		return new ExportedFile(content, name + "." + format.extension(), format.contentType());
	}
}
