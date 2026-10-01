package br.gravita.core.ports.outbound.reporting;

import java.util.List;

/**
 * Renders a tabular report as an Excel workbook (xlsx), one worksheet per {@link Sheet}. Like {@link RenderPdfPort}
 * it knows nothing of any one report. Cells keep their type so the spreadsheet can be summed and sorted: a
 * {@code String}, a {@code BigDecimal} or other {@code Number}, a {@code LocalDate}, or {@code null} for an empty cell.
 */
public interface RenderExcelPort {

	byte[] render(ExcelWorkbook workbook);

	/** {@code headerLines} are written under the title at the top of every sheet. */
	record ExcelWorkbook(String title, List<String> headerLines, List<Sheet> sheets) {
	}

	/** {@code footerLines} are written under the last row, e.g. its totals. */
	record Sheet(String name, List<String> columns, List<List<Object>> rows, List<String> footerLines) {
	}
}
