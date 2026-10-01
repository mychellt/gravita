package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.AbcClass;
import br.gravita.core.ports.inbound.reporting.AbcCurveEntry;
import br.gravita.core.ports.inbound.reporting.AbcCurveQuery;
import br.gravita.core.ports.inbound.reporting.AbcCurveType;
import br.gravita.core.ports.inbound.reporting.AssessedTaxSummary;
import br.gravita.core.ports.inbound.reporting.AssessedTaxesQuery;
import br.gravita.core.ports.inbound.reporting.CommissionReportEntry;
import br.gravita.core.ports.inbound.reporting.CommissionReportQuery;
import br.gravita.core.ports.inbound.reporting.DashboardPeriod;
import br.gravita.core.ports.inbound.reporting.DashboardQuery;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView;
import br.gravita.core.ports.inbound.reporting.ExportFormat;
import br.gravita.core.ports.inbound.reporting.ExportReportQuery;
import br.gravita.core.ports.inbound.reporting.ExportedFile;
import br.gravita.core.ports.inbound.reporting.FiscalBookEntry;
import br.gravita.core.ports.inbound.reporting.FiscalBookFlow;
import br.gravita.core.ports.inbound.reporting.FiscalBooks;
import br.gravita.core.ports.inbound.reporting.FiscalBooksQuery;
import br.gravita.core.ports.inbound.reporting.GetAbcCurveUseCase;
import br.gravita.core.ports.inbound.reporting.GetAssessedTaxesUseCase;
import br.gravita.core.ports.inbound.reporting.GetCommissionReportUseCase;
import br.gravita.core.ports.inbound.reporting.GetExecutiveDashboardUseCase;
import br.gravita.core.ports.inbound.reporting.GetFiscalBooksUseCase;
import br.gravita.core.ports.inbound.reporting.GetStockTurnoverUseCase;
import br.gravita.core.ports.inbound.reporting.ReportId;
import br.gravita.core.ports.inbound.reporting.StockTurnoverEntry;
import br.gravita.core.ports.inbound.reporting.StockTurnoverQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.RenderExcelPort;
import br.gravita.core.ports.outbound.reporting.RenderExcelPort.ExcelWorkbook;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.usercases.reporting.ExportReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExportReportServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final byte[] PDF = "pdf".getBytes();
	private static final byte[] XLSX = "xlsx".getBytes();

	private final GetExecutiveDashboardUseCase dashboard = mock(GetExecutiveDashboardUseCase.class);
	private final GetAbcCurveUseCase abcCurve = mock(GetAbcCurveUseCase.class);
	private final GetStockTurnoverUseCase stockTurnover = mock(GetStockTurnoverUseCase.class);
	private final GetCommissionReportUseCase commissions = mock(GetCommissionReportUseCase.class);
	private final GetFiscalBooksUseCase fiscalBooks = mock(GetFiscalBooksUseCase.class);
	private final GetAssessedTaxesUseCase assessedTaxes = mock(GetAssessedTaxesUseCase.class);
	private final RenderPdfPort pdf = mock(RenderPdfPort.class);
	private final RenderExcelPort excel = mock(RenderExcelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private final UUID product = UUID.randomUUID();
	private ExportReportService service;

	@BeforeEach
	void setUp() {
		service = new ExportReportService(dashboard, abcCurve, stockTurnover, commissions, fiscalBooks, assessedTaxes,
				pdf, excel, permissions);
		when(permissions.canExport(any(), any())).thenReturn(false);
		for (ReportId report : ReportId.values()) {
			when(permissions.canExport(user, report.slug())).thenReturn(true);
		}
		when(pdf.render(any())).thenReturn(PDF);
		when(excel.render(any())).thenReturn(XLSX);
	}

	@DisplayName("Refuses a user without export permission, without running or rendering the report")
	@Test
	void refusesAUserWithoutTheExportPermissionOnTheReportWithoutRunningItOrRendering() {
		UserId viewer = UserId.generate();
		when(permissions.canExport(viewer, "abc-curve")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(abcQuery(viewer, ExportFormat.PDF)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(abcCurve, pdf, excel);
	}

	@DisplayName("Checks the export permission of the report being exported, not another report's")
	@Test
	void checksTheExportPermissionOfTheReportBeingExportedNotAnotherOne() {
		when(permissions.canExport(user, "abc-curve")).thenReturn(false);
		when(permissions.canExport(user, "commissions")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(abcQuery(user, ExportFormat.XLSX)))
				.isInstanceOf(ForbiddenException.class);
		verify(permissions).canExport(user, "abc-curve");
	}

	@DisplayName("Exports the ABC curve for the requested parameters as PDF or Excel from the same layout")
	@Test
	void exportsTheAbcCurveOfTheRequestedParametersAsPdfOrExcelFromTheSameLayout() {
		UUID company = UUID.randomUUID();
		when(abcCurve.execute(new AbcCurveQuery(user, AbcCurveType.PRODUCT, PERIOD, company)))
				.thenReturn(List.of(new AbcCurveEntry(product, new BigDecimal("900.00"), new BigDecimal("90.00"),
						new BigDecimal("90.00"), AbcClass.A)));

		ExportedFile asPdf = service.execute(new ExportReportQuery(user, ReportId.ABC_CURVE, ExportFormat.PDF, PERIOD,
				null, AbcCurveType.PRODUCT, company, null));
		ExportedFile asExcel = service.execute(new ExportReportQuery(user, ReportId.ABC_CURVE, ExportFormat.XLSX,
				PERIOD, null, AbcCurveType.PRODUCT, company, null));

		assertThat(asPdf.content()).isEqualTo(PDF);
		assertThat(asPdf.filename()).isEqualTo("abc-curve-product-2028-02.pdf");
		assertThat(asPdf.contentType()).isEqualTo("application/pdf");
		assertThat(asExcel.content()).isEqualTo(XLSX);
		assertThat(asExcel.filename()).isEqualTo("abc-curve-product-2028-02.xlsx");
		assertThat(asExcel.contentType())
				.isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		ArgumentCaptor<PdfReport> report = ArgumentCaptor.forClass(PdfReport.class);
		verify(pdf).render(report.capture());
		assertThat(report.getValue().title()).isEqualTo("Curva ABC - Produtos - 02/2028");
		assertThat(report.getValue().headerLines()).containsExactly("Período: 01/02/2028 a 29/02/2028");
		assertThat(report.getValue().sections()).hasSize(1);
		assertThat(report.getValue().sections().get(0).rows())
				.containsExactly(List.of("1", product.toString(), "900,00", "90,00", "90,00", "A"));

		ArgumentCaptor<ExcelWorkbook> workbook = ArgumentCaptor.forClass(ExcelWorkbook.class);
		verify(excel).render(workbook.capture());
		assertThat(workbook.getValue().sheets().get(0).rows().get(0))
				.containsExactly(1, product, new BigDecimal("900.00"), new BigDecimal("90.00"),
						new BigDecimal("90.00"), "A");
	}

	@DisplayName("Exports the dashboard of the period, defaulting to the month")
	@Test
	void exportsTheDashboardOfThePeriodDefaultingToTheMonth() {
		when(dashboard.execute(any())).thenReturn(dashboardView());

		ExportedFile file = service.execute(new ExportReportQuery(user, ReportId.DASHBOARD, ExportFormat.PDF, null,
				null, null, null, null));

		verify(dashboard).execute(new DashboardQuery(user, DashboardPeriod.MONTH, null));
		assertThat(file.filename()).isEqualTo("dashboard-month.pdf");
		ArgumentCaptor<PdfReport> report = ArgumentCaptor.forClass(PdfReport.class);
		verify(pdf).render(report.capture());
		assertThat(report.getValue().sections()).extracting(section -> section.heading()).containsExactly("Faturamento",
				"CMV e Margem", "Inadimplência", "Estoque crítico", "Top produtos por quantidade",
				"Top produtos por valor", "Metas de vendas - 02/2028");
		assertThat(report.getValue().sections().get(0).rows()).containsExactly(
				List.of("Dia", "100,00", "50,00", "100,00"), List.of("Semana", "200,00", "0,00", "-"),
				List.of("Mês", "300,00", "300,00", "0,00"));
		assertThat(report.getValue().sections().get(3).rows().get(0))
				.containsExactly(product.toString(), "Abaixo do mínimo", "2,000", "5,000", "-", "-", "-");
	}

	@DisplayName("Exports the stock turnover and commission reports with their filters")
	@Test
	void exportsTheStockTurnoverAndTheCommissionsWithTheirFilters() {
		UUID salesperson = UUID.randomUUID();
		when(stockTurnover.execute(any())).thenReturn(List.of(new StockTurnoverEntry(product, null, true)));
		when(commissions.execute(any())).thenReturn(List.of(
				new CommissionReportEntry(salesperson, product, UUID.randomUUID(), new BigDecimal("5.00"),
						new BigDecimal("10.00")),
				new CommissionReportEntry(salesperson, product, UUID.randomUUID(), new BigDecimal("5.00"),
						new BigDecimal("2.50"))));

		ExportedFile turnover = service.execute(new ExportReportQuery(user, ReportId.STOCK_TURNOVER, ExportFormat.XLSX,
				PERIOD, null, null, null, null));
		ExportedFile commission = service.execute(new ExportReportQuery(user, ReportId.COMMISSIONS, ExportFormat.PDF,
				PERIOD, null, null, null, salesperson));

		verify(stockTurnover).execute(new StockTurnoverQuery(user, PERIOD, null));
		verify(commissions).execute(new CommissionReportQuery(user, salesperson, PERIOD));
		assertThat(turnover.filename()).isEqualTo("stock-turnover-2028-02.xlsx");
		assertThat(commission.filename()).isEqualTo("commissions-2028-02.pdf");
		ArgumentCaptor<PdfReport> report = ArgumentCaptor.forClass(PdfReport.class);
		verify(pdf).render(report.capture());
		assertThat(report.getValue().sections().get(0).footerLines())
				.containsExactly("Lançamentos: 2 | Total de comissões: 12,50");
		ArgumentCaptor<ExcelWorkbook> workbook = ArgumentCaptor.forClass(ExcelWorkbook.class);
		verify(excel).render(workbook.capture());
		assertThat(workbook.getValue().sheets().get(0).rows().get(0)).containsExactly(product, null, "Sim");
	}

	@DisplayName("Exports the assessed taxes report")
	@Test
	void exportsTheAssessedTaxes() {
		when(assessedTaxes.execute(new AssessedTaxesQuery(user, PERIOD))).thenReturn(new AssessedTaxSummary(PERIOD,
				new BigDecimal("150.50"), new BigDecimal("15.25"), new BigDecimal("2.48"), new BigDecimal("11.40"),
				new BigDecimal("30.00")));

		ExportedFile file = service.execute(new ExportReportQuery(user, ReportId.ASSESSED_TAXES, ExportFormat.PDF,
				PERIOD, null, null, null, null));

		assertThat(file.filename()).isEqualTo("assessed-taxes-2028-02.pdf");
		ArgumentCaptor<PdfReport> report = ArgumentCaptor.forClass(PdfReport.class);
		verify(pdf).render(report.capture());
		assertThat(report.getValue().sections().get(0).rows()).containsExactly(List.of("ICMS", "150,50"),
				List.of("IPI", "15,25"), List.of("PIS", "2,48"), List.of("COFINS", "11,40"), List.of("ISS", "30,00"));
	}

	@DisplayName("Returns the fiscal books' statutory PDF as is and lays out their Excel")
	@Test
	void returnsTheStatutoryPdfOfTheFiscalBooksAsIsAndLaysOutTheirExcel() {
		FiscalBookEntry exit = new FiscalBookEntry(FiscalBookFlow.EXIT, LocalDate.of(2028, 2, 10), "NFE", "1", "20",
				"key", "Cliente", "123", "5102", new BigDecimal("1000.00"), new BigDecimal("180.00"));
		byte[] statutoryPdf = "books".getBytes();
		when(fiscalBooks.execute(new FiscalBooksQuery(user, PERIOD))).thenReturn(new FiscalBooks(PERIOD, List.of(),
				List.of(exit), List.of(exit), new BigDecimal("180.00"), BigDecimal.ZERO, new BigDecimal("180.00"),
				statutoryPdf, new byte[0]));

		ExportedFile asPdf = service.execute(new ExportReportQuery(user, ReportId.FISCAL_BOOKS, ExportFormat.PDF,
				PERIOD, null, null, null, null));
		ExportedFile asExcel = service.execute(new ExportReportQuery(user, ReportId.FISCAL_BOOKS, ExportFormat.XLSX,
				PERIOD, null, null, null, null));

		assertThat(asPdf.content()).isEqualTo(statutoryPdf);
		assertThat(asPdf.filename()).isEqualTo("fiscal-books-2028-02.pdf");
		verifyNoInteractions(pdf);
		assertThat(asExcel.content()).isEqualTo(XLSX);
		ArgumentCaptor<ExcelWorkbook> workbook = ArgumentCaptor.forClass(ExcelWorkbook.class);
		verify(excel).render(workbook.capture());
		assertThat(workbook.getValue().sheets()).extracting(sheet -> sheet.name())
				.containsExactly("Livro de Entradas", "Livro de Saídas", "Livro de Apuração do ICMS");
		assertThat(workbook.getValue().sheets().get(1).rows()).hasSize(1);
		assertThat(workbook.getValue().sheets().get(1).rows().get(0).get(0)).isEqualTo(LocalDate.of(2028, 2, 10));
		assertThat(workbook.getValue().sheets().get(2).footerLines()).containsExactly("Débitos (saídas): 180,00",
				"Créditos (entradas): 0,00", "Saldo (débitos - créditos): 180,00");
	}

	@DisplayName("Refuses to export reports that have nothing to run yet")
	@Test
	void refusesToExportTheReportsThatHaveNothingToRunYet() {
		for (ReportId report : List.of(ReportId.DRE, ReportId.PURCHASES_BY_SUPPLIER)) {
			assertThatThrownBy(() -> service.execute(new ExportReportQuery(user, report, ExportFormat.PDF, PERIOD,
					null, null, null, null))).isInstanceOf(ResourceNotFoundException.class);
		}
		verifyNoInteractions(pdf, excel);
	}

	@DisplayName("Does not swallow what the report itself refuses")
	@Test
	void doesNotCatchWhatTheReportItselfRefuses() {
		when(abcCurve.execute(any())).thenThrow(new ForbiddenException("cannot view"));

		assertThatThrownBy(() -> service.execute(abcQuery(user, ExportFormat.PDF)))
				.isInstanceOf(ForbiddenException.class);
		verifyNoInteractions(pdf);
	}

	@DisplayName("Requires the period and the ABC type for the reports that take them")
	@Test
	void requiresThePeriodAndTheAbcTypeWhereTheReportTakesThem() {
		assertThatThrownBy(() -> new ExportReportQuery(user, ReportId.COMMISSIONS, ExportFormat.PDF, null, null, null,
				null, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("period");
		assertThatThrownBy(() -> new ExportReportQuery(user, ReportId.ABC_CURVE, ExportFormat.PDF, PERIOD, null, null,
				null, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("type");
	}

	private ExportReportQuery abcQuery(UserId requester, ExportFormat format) {
		return new ExportReportQuery(requester, ReportId.ABC_CURVE, format, PERIOD, null, AbcCurveType.PRODUCT, null,
				null);
	}

	private ExecutiveDashboardView dashboardView() {
		return new ExecutiveDashboardView(DashboardPeriod.MONTH, LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29),
				new ExecutiveDashboardView.Revenue(
						new ExecutiveDashboardView.PeriodComparison(new BigDecimal("100.00"), new BigDecimal("50.00"),
								new BigDecimal("100.00")),
						new ExecutiveDashboardView.PeriodComparison(new BigDecimal("200.00"), BigDecimal.ZERO.setScale(2),
								null),
						new ExecutiveDashboardView.PeriodComparison(new BigDecimal("300.00"), new BigDecimal("300.00"),
								new BigDecimal("0.00")),
						new BigDecimal("300.00"), new BigDecimal("0.00")),
				new ExecutiveDashboardView.Margin(new BigDecimal("300.00"), new BigDecimal("200.00"),
						new BigDecimal("100.00"), new BigDecimal("33.33")),
				new ExecutiveDashboardView.Delinquency(new BigDecimal("10.00"), 1,
						new ExecutiveDashboardView.AgingBuckets(new BigDecimal("10.00"), BigDecimal.ZERO,
								BigDecimal.ZERO)),
				List.of(new ExecutiveDashboardView.CriticalStockItem(product,
						ExecutiveDashboardView.CriticalStockReason.BELOW_MINIMUM, new BigDecimal("2.000"),
						new BigDecimal("5.000"), null, null, null)),
				new ExecutiveDashboardView.TopProducts(
						List.of(new ExecutiveDashboardView.TopProduct(product, new BigDecimal("3"), new BigDecimal("30.00"))),
						List.of()),
				new ExecutiveDashboardView.TargetProgress(PERIOD,
						new ExecutiveDashboardView.Target(new BigDecimal("1000.00"), new BigDecimal("300.00"),
								new BigDecimal("30.00")),
						List.of()));
	}
}
