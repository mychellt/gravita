package br.gravita.core.usercases.reporting;

import br.gravita.core.ports.outbound.reporting.RenderExcelPort.ExcelWorkbook;
import br.gravita.core.ports.outbound.reporting.RenderExcelPort.Sheet;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Column;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Section;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * A report laid out once as titled tables of typed cells, so the PDF and the Excel export of a report show the same
 * rows and the same totals. The PDF prints the cells as Brazilian-formatted text ({@code 31/03/2028},
 * {@code 1.234,56}); the Excel keeps them typed so the figures can still be summed. Cells are {@code String},
 * {@code BigDecimal}, {@code Integer}/{@code Long}, {@code LocalDate} or {@code null}.
 */
record ExportLayout(String title, List<String> headerLines, List<Table> tables) {

	private static final Locale PT_BR = Locale.of("pt", "BR");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final String NONE = "-";

	/** {@code weight} is the column's share of the PDF page width relative to the other columns. */
	record Col(String header, int weight, boolean rightAligned) {
	}

	record Table(String heading, List<Col> columns, List<List<Object>> rows, List<String> footerLines) {
	}

	PdfReport pdfReport() {
		return new PdfReport(title, headerLines, tables.stream().map(ExportLayout::section).toList());
	}

	ExcelWorkbook excelWorkbook() {
		return new ExcelWorkbook(title, headerLines, tables.stream().map(ExportLayout::sheet).toList());
	}

	private static Section section(final Table table) {
		final List<Column> columns = table.columns().stream()
				.map(col -> new Column(col.header(), col.weight(), col.rightAligned())).toList();
		final List<List<String>> rows = table.rows().stream()
				.map(row -> row.stream().map(ExportLayout::text).toList()).toList();
		return new Section(table.heading(), columns, rows, table.footerLines());
	}

	private static Sheet sheet(final Table table) {
		return new Sheet(table.heading(), table.columns().stream().map(Col::header).toList(), table.rows(),
				table.footerLines());
	}

	/** Amounts keep the scale they were reported with. */
	static String text(final Object cell) {
		return switch (cell) {
			case null -> NONE;
			case String value -> value.isBlank() ? NONE : value;
			case BigDecimal value -> number(value);
			case LocalDate value -> value.format(DATE);
			default -> cell.toString();
		};
	}

	/** A total shown in the footers, always with cents even when it sums nothing. */
	static String money(final BigDecimal value) {
		return number(value.setScale(2, RoundingMode.HALF_UP));
	}

	static String number(final BigDecimal value) {
		final NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
		final int scale = Math.max(0, value.scale());
		format.setMinimumFractionDigits(scale);
		format.setMaximumFractionDigits(scale);
		return format.format(value);
	}
}
