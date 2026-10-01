package br.gravita.adapters.outbound.rendering.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.ports.outbound.reporting.RenderExcelPort.ExcelWorkbook;
import br.gravita.core.ports.outbound.reporting.RenderExcelPort.Sheet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PoiExcelReportAdapterTest {

	private final PoiExcelReportAdapter adapter = new PoiExcelReportAdapter();

	@Test
	@DisplayName("Writes the title, header lines, column headers, rows and footers of each sheet")
	void writesTheTitleHeaderLinesColumnHeadersRowsAndFootersOfEachSheet() throws IOException {
		byte[] xlsx = adapter.render(new ExcelWorkbook("Livros Fiscais - 02/2028",
				List.of("Período: 01/02/2028 a 29/02/2028"),
				List.of(new Sheet("Livro de Saídas", List.of("Documento", "Valor"),
						List.of(row("NFE 1/20", new BigDecimal("1000.50"))), List.of("Documentos: 1")))));

		try (Workbook workbook = open(xlsx)) {
			org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("Livro de Saídas");
			assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Livros Fiscais - 02/2028");
			assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Período: 01/02/2028 a 29/02/2028");
			assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Documento");
			assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("Valor");
			assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("NFE 1/20");
			assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Documentos: 1");
		}
	}

	@Test
	@DisplayName("Keeps amounts and dates as real cells so the spreadsheet can be summed")
	void keepsAmountsAndDatesAsRealCellsSoTheSpreadsheetCanBeSummed() throws IOException {
		byte[] xlsx = adapter.render(new ExcelWorkbook("T", List.of(), List.of(new Sheet("S",
				List.of("Data", "Valor", "Qtd", "Vazio", "Texto"),
				List.of(row(LocalDate.of(2028, 2, 29), new BigDecimal("1234.5"), 3, null, "x")), List.of()))));

		try (Workbook workbook = open(xlsx)) {
			org.apache.poi.ss.usermodel.Row row = workbook.getSheet("S").getRow(3);
			assertThat(row.getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2028, 2, 29));
			assertThat(row.getCell(0).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
			assertThat(row.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
			assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(1234.5);
			assertThat(row.getCell(1).getCellStyle().getDataFormatString()).isEqualTo("#,##0.0");
			assertThat(row.getCell(2).getNumericCellValue()).isEqualTo(3);
			assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.BLANK);
			assertThat(row.getCell(4).getStringCellValue()).isEqualTo("x");
		}
	}

	@Test
	@DisplayName("Writes text that looks like a formula as plain text, never evaluating it")
	void writesTextThatLooksLikeAFormulaAsTextNeverEvaluatingIt() throws IOException {
		byte[] xlsx = adapter.render(new ExcelWorkbook("T", List.of(), List.of(new Sheet("S", List.of("Nome"),
				List.of(row("=1+1")), List.of()))));

		try (Workbook workbook = open(xlsx)) {
			assertThat(workbook.getSheet("S").getRow(3).getCell(0).getCellType()).isEqualTo(CellType.STRING);
			assertThat(workbook.getSheet("S").getRow(3).getCell(0).getStringCellValue()).isEqualTo("=1+1");
		}
	}

	@Test
	@DisplayName("Makes sheet names valid and unique so no input fails the render")
	void makesSheetNamesValidAndUniqueSoNoInputFailsTheRender() throws IOException {
		String longName = "Livro de Apuração do ICMS com um nome muito longo";
		byte[] xlsx = adapter.render(new ExcelWorkbook("T", List.of(), List.of(
				new Sheet("Entradas/Saídas: [1]", List.of("A"), List.of(), List.of()),
				new Sheet("entradas/saídas: [1]", List.of("A"), List.of(), List.of()),
				new Sheet(longName, List.of("A"), List.of(), List.of()),
				new Sheet("", List.of("A"), List.of(), List.of()))));

		try (Workbook workbook = open(xlsx)) {
			List<String> names = new ArrayList<>();
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				names.add(workbook.getSheetName(i));
			}
			assertThat(names).hasSize(4).doesNotHaveDuplicates().allMatch(name -> name.length() <= 31);
			assertThat(names.get(0)).isEqualTo("Entradas Saídas   1");
			assertThat(names.get(2)).isEqualTo(longName.substring(0, 31));
			assertThat(names.get(3)).isEqualTo("Relatório");
		}
	}

	@Test
	@DisplayName("Renders a workbook without sheets as one empty sheet")
	void rendersAWorkbookWithoutSheetsAsOneEmptySheet() throws IOException {
		byte[] xlsx = adapter.render(new ExcelWorkbook("Vazio", List.of(), List.of()));

		try (Workbook workbook = open(xlsx)) {
			assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
		}
	}

	private static Workbook open(byte[] xlsx) throws IOException {
		return new XSSFWorkbook(new ByteArrayInputStream(xlsx));
	}

	private static List<Object> row(Object... cells) {
		return Arrays.asList(cells);
	}
}
