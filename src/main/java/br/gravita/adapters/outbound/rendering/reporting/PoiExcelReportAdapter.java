package br.gravita.adapters.outbound.rendering.reporting;

import br.gravita.core.ports.outbound.reporting.RenderExcelPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Renders an {@link ExcelWorkbook} with Apache POI: each {@link Sheet} gets the report title and header lines, a
 * bold column-header row, its rows with numbers and dates as real cells (an amount shows with as many decimals as its
 * scale, a date as {@code dd/mm/yyyy}), and its footer lines. Text is always written as a string cell, so a value that
 * starts with {@code =} is shown, never evaluated as a formula. Sheet names are made valid and unique, so no input
 * can fail the render.
 */
@Component
public class PoiExcelReportAdapter implements RenderExcelPort {

	private static final int MAX_SHEET_NAME = 31;
	private static final int MIN_COLUMN_CHARS = 8;
	private static final int MAX_COLUMN_CHARS = 60;
	private static final String DATE_FORMAT = "dd/mm/yyyy";

	@Override
	public byte[] render(ExcelWorkbook workbook) {
		try (XSSFWorkbook xlsx = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Styles styles = new Styles(xlsx);
			Set<String> usedNames = new HashSet<>();
			for (Sheet sheet : workbook.sheets()) {
				writeSheet(xlsx, styles, workbook, sheet, uniqueName(sheet.name(), usedNames));
			}
			if (workbook.sheets().isEmpty()) {
				xlsx.createSheet(sanitize(workbook.title()));
			}
			xlsx.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to render the Excel report " + workbook.title(), e);
		}
	}

	private void writeSheet(Workbook xlsx, Styles styles, ExcelWorkbook workbook, Sheet sheet, String name) {
		org.apache.poi.ss.usermodel.Sheet worksheet = xlsx.createSheet(name);
		int[] widths = new int[sheet.columns().size()];
		int rowIndex = 0;
		text(worksheet.createRow(rowIndex++), 0, workbook.title(), styles.title());
		for (String line : workbook.headerLines()) {
			text(worksheet.createRow(rowIndex++), 0, line, null);
		}
		rowIndex++;
		int headerRow = rowIndex++;
		Row header = worksheet.createRow(headerRow);
		for (int i = 0; i < widths.length; i++) {
			text(header, i, sheet.columns().get(i), styles.header());
			widths[i] = sheet.columns().get(i).length();
		}
		for (List<Object> values : sheet.rows()) {
			Row row = worksheet.createRow(rowIndex++);
			for (int i = 0; i < widths.length; i++) {
				widths[i] = Math.max(widths[i], write(row, i, values.get(i), styles));
			}
		}
		if (!sheet.footerLines().isEmpty()) {
			rowIndex++;
			for (String line : sheet.footerLines()) {
				text(worksheet.createRow(rowIndex++), 0, line, styles.header());
			}
		}
		for (int i = 0; i < widths.length; i++) {
			int chars = Math.min(MAX_COLUMN_CHARS, Math.max(MIN_COLUMN_CHARS, widths[i] + 2));
			worksheet.setColumnWidth(i, chars * 256);
		}
		worksheet.createFreezePane(0, headerRow + 1);
	}

	/** Writes one cell and returns how many characters wide it is shown. */
	private int write(Row row, int column, Object value, Styles styles) {
		Cell cell = row.createCell(column);
		switch (value) {
			case null -> {
				return 0;
			}
			case BigDecimal amount -> {
				cell.setCellValue(amount.doubleValue());
				cell.setCellStyle(styles.number(amount.scale()));
				return amount.toPlainString().length() + 2;
			}
			case Number number -> {
				cell.setCellValue(number.doubleValue());
				return number.toString().length();
			}
			case LocalDate date -> {
				cell.setCellValue(date);
				cell.setCellStyle(styles.date());
				return DATE_FORMAT.length();
			}
			default -> {
				String text = value.toString();
				cell.setCellValue(text);
				return text.length();
			}
		}
	}

	private static void text(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value);
		if (style != null) {
			cell.setCellStyle(style);
		}
	}

	/** Excel allows 31 characters and none of {@code : \ / ? * [ ]}; a repeated name gets a numeric suffix. */
	private static String uniqueName(String wanted, Set<String> used) {
		String base = sanitize(wanted);
		String name = base;
		for (int suffix = 2; !used.add(name.toLowerCase()); suffix++) {
			String tail = " (" + suffix + ")";
			name = base.substring(0, Math.min(base.length(), MAX_SHEET_NAME - tail.length())) + tail;
		}
		return name;
	}

	private static String sanitize(String wanted) {
		String name = (wanted == null ? "" : wanted).replaceAll("[:\\\\/?*\\[\\]]", " ").strip();
		name = name.replaceAll("^'+|'+$", "");
		if (name.isEmpty()) {
			name = "Relatório";
		}
		return name.substring(0, Math.min(name.length(), MAX_SHEET_NAME));
	}

	private static final class Styles {

		private final Workbook workbook;
		private final CellStyle title;
		private final CellStyle header;
		private final CellStyle date;
		private final Map<Integer, CellStyle> numbers = new HashMap<>();

		Styles(Workbook workbook) {
			this.workbook = workbook;
			this.title = bold(14);
			this.header = bold(10);
			CreationHelper helper = workbook.getCreationHelper();
			this.date = workbook.createCellStyle();
			this.date.setDataFormat(helper.createDataFormat().getFormat(DATE_FORMAT));
		}

		CellStyle title() {
			return title;
		}

		CellStyle header() {
			return header;
		}

		CellStyle date() {
			return date;
		}

		/** An amount shows with the decimals it was reported with, and a thousands separator. */
		CellStyle number(int scale) {
			int decimals = Math.max(0, scale);
			return numbers.computeIfAbsent(decimals, key -> {
				CellStyle style = workbook.createCellStyle();
				String format = key == 0 ? "#,##0" : "#,##0." + "0".repeat(key);
				style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(format));
				return style;
			});
		}

		private CellStyle bold(int size) {
			Font font = workbook.createFont();
			font.setBold(true);
			font.setFontHeightInPoints((short) size);
			CellStyle style = workbook.createCellStyle();
			style.setFont(font);
			return style;
		}
	}
}
