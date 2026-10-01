package br.gravita.reporting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the real controller, export and report services, read-model adapter and both renderers over the real
 * repositories, exporting the stock turnover as the screen's example of any report; only session and permission are
 * stubbed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ExportReportEndToEndTest {

	private static final UUID WAREHOUSE = UUID.randomUUID();
	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Autowired
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final UUID rice = UUID.randomUUID();
	private final UUID beans = UUID.randomUUID();

	@BeforeEach
	void seed() {
		UserId exporter = UserId.generate();
		UserId viewer = UserId.generate();
		when(sessionStorePort.resolve("exporter-token")).thenReturn(Optional.of(exporter));
		when(sessionStorePort.resolve("viewer-token")).thenReturn(Optional.of(viewer));
		grant(exporter, "stock-turnover", PermissionAction.VIEW);
		grant(exporter, "stock-turnover", PermissionAction.EXPORT);
		grant(exporter, "dre", PermissionAction.EXPORT);
		grant(viewer, "stock-turnover", PermissionAction.VIEW);

		// Rice: 10 on hand on 2019-03-01, 30 bought and 20 sold in March, 5 more sold in April, 15 on hand today. Beans: 40, nothing issued.
		balance(rice, "15");
		movement(rice, StockMovementType.ENTRY, "30", "2019-03-05T10:00:00Z");
		movement(rice, StockMovementType.EXIT, "20", "2019-03-20T10:00:00Z");
		movement(rice, StockMovementType.EXIT, "5", "2019-04-02T10:00:00Z");
		balance(beans, "40");
	}

	@DisplayName("Exports the screen's rows as an Excel file with typed cells")
	@Test
	void exportsTheScreensRowsAsAnExcelFileWithTypedCells() throws Exception {
		byte[] body = mockMvc.perform(get("/api/reports/stock-turnover/export").param("format", "xlsx")
				.param("period", "2019-03").header("Authorization", "Bearer exporter-token"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", XLSX))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"stock-turnover-2019-03.xlsx\""))
				.andReturn().getResponse().getContentAsByteArray();

		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(body))) {
			Sheet sheet = workbook.getSheet("Giro de Estoque");
			assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Giro de Estoque - 03/2019");
			assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Produto");
			assertThat(row(sheet, rice).getCell(1).getNumericCellValue()).isEqualTo(1.3333);
			assertThat(row(sheet, rice).getCell(2).getStringCellValue()).isEqualTo("Não");
			assertThat(row(sheet, beans).getCell(1).getNumericCellValue()).isEqualTo(0.0);
			assertThat(row(sheet, beans).getCell(2).getStringCellValue()).isEqualTo("Sim");
		}
	}

	@DisplayName("Exports the same rows as a PDF file")
	@Test
	void exportsTheSameRowsAsAPdfFile() throws Exception {
		byte[] body = mockMvc.perform(get("/api/reports/stock-turnover/export").param("format", "pdf")
				.param("period", "2019-03").header("Authorization", "Bearer exporter-token"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andExpect(header().string("Content-Disposition",
						"attachment; filename=\"stock-turnover-2019-03.pdf\""))
				.andReturn().getResponse().getContentAsByteArray();

		try (PDDocument pdf = Loader.loadPDF(body)) {
			String text = new PDFTextStripper().getText(pdf);
			assertThat(text).contains("Giro de Estoque - 03/2019", "Período: 01/03/2019 a 31/03/2019", rice.toString(),
					"1,3333", beans.toString(), "0,0000");
		}
	}

	@DisplayName("Returns 403 when the profile can view the report but not export it")
	@Test
	void answersForbiddenWhenTheProfileCanViewTheReportButNotExportIt() throws Exception {
		mockMvc.perform(get("/api/reports/stock-turnover/export").param("format", "pdf").param("period", "2019-03")
				.header("Authorization", "Bearer viewer-token")).andExpect(status().isForbidden());
	}

	@DisplayName("Returns 400 for an unknown format or a missing parameter")
	@Test
	void answersBadRequestForAnUnknownFormatOrAMissingParameter() throws Exception {
		mockMvc.perform(get("/api/reports/stock-turnover/export").param("format", "csv").param("period", "2019-03")
				.header("Authorization", "Bearer exporter-token")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/stock-turnover/export").param("format", "pdf")
				.header("Authorization", "Bearer exporter-token")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/abc-curve/export").param("format", "pdf").param("period", "2019-03")
				.header("Authorization", "Bearer exporter-token")).andExpect(status().isBadRequest());
	}

	@DisplayName("Returns 404 for an unknown report and for reports not available yet")
	@Test
	void answersNotFoundForAnUnknownReportAndForOnesNotAvailableYet() throws Exception {
		mockMvc.perform(get("/api/reports/payroll/export").param("format", "pdf").param("period", "2019-03")
				.header("Authorization", "Bearer exporter-token")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/reports/dre/export").param("format", "pdf").param("period", "2019-03")
				.header("Authorization", "Bearer exporter-token")).andExpect(status().isNotFound());
	}

	@DisplayName("Returns 401 when there is no session")
	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/stock-turnover/export").param("format", "pdf").param("period", "2019-03"))
				.andExpect(status().isUnauthorized());
	}

	private void grant(UserId user, String screen, PermissionAction action) {
		when(checkPermissionUseCase.execute(argThat(query -> query != null && query.userId().equals(user)
				&& query.module().equals("reporting") && query.screen().equals(screen)
				&& query.action() == action))).thenReturn(true);
	}

	private static org.apache.poi.ss.usermodel.Row row(Sheet sheet, UUID product) {
		for (org.apache.poi.ss.usermodel.Row row : sheet) {
			if (row.getCell(0) != null && row.getCell(0).getStringCellValue().equals(product.toString())) {
				return row;
			}
		}
		throw new AssertionError("No row for " + product);
	}

	private void balance(UUID product, String onHand) {
		stockBalanceRepositoryPort.save(StockBalance.of(StockBalanceId.of(UUID.randomUUID()), product, WAREHOUSE,
				new BigDecimal(onHand), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN));
	}

	private void movement(UUID product, StockMovementType type, String quantity, String at) {
		stockMovementRepositoryPort.save(StockMovement.of(StockMovementId.of(UUID.randomUUID()), type, product,
				WAREHOUSE, new BigDecimal(quantity), BigDecimal.TEN, null, List.of(), "TEST", null, UUID.randomUUID(),
				Instant.parse(at)));
	}
}
