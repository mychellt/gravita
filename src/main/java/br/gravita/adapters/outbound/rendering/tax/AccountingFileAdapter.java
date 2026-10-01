package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.ports.inbound.tax.AccountingEntry;
import br.gravita.core.ports.inbound.tax.AccountingExportFormat;
import br.gravita.core.ports.outbound.tax.ExportAccountingFilePort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Writes the accounting entries of a period (UC-M2-14) in the one layout available until M1/M10 can configure an
 * accounting system's own. Both files are UTF-8 with CRLF line ends, one line per entry, and print dates and amounts
 * the Brazilian way ({@code 31/03/2028}, {@code 1234,56} - no thousands separator, so an importer reads them back
 * unambiguously).
 *
 * <p>The CSV is semicolon-separated, opens with a header row and quotes a cell that holds a separator, a quote or a
 * line break. A text cell that starts with {@code = + - @} is prefixed with a quote so a spreadsheet reads it as text,
 * not as a formula: supplier and recipient names come from documents the company did not write. The TXT has no header
 * and fixed-width fields separated by one space: text left-aligned, amounts right-aligned, the participant's name cut
 * to its width.
 */
@Component
class AccountingFileAdapter implements ExportAccountingFilePort {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final String EOL = "\r\n";
	private static final char CSV_SEPARATOR = ';';

	private static final List<String> CSV_HEADER = List.of("Data", "Natureza", "Série", "Número", "Chave de acesso",
			"Participante", "CNPJ/CPF", "CFOP", "Valor total", "ICMS", "IPI", "PIS", "COFINS");

	private static final int FLOW_WIDTH = 7;
	private static final int SERIES_WIDTH = 3;
	private static final int NUMBER_WIDTH = 9;
	private static final int ACCESS_KEY_WIDTH = 44;
	private static final int NAME_WIDTH = 40;
	private static final int DOCUMENT_WIDTH = 14;
	private static final int CFOP_WIDTH = 15;
	private static final int AMOUNT_WIDTH = 15;

	@Override
	public byte[] export(List<AccountingEntry> entries, AccountingExportFormat format) {
		String content = switch (format) {
			case CSV -> csv(entries);
			case TXT -> txt(entries);
		};
		return content.getBytes(StandardCharsets.UTF_8);
	}

	private static String csv(List<AccountingEntry> entries) {
		StringBuilder csv = new StringBuilder();
		appendCsvRow(csv, CSV_HEADER);
		for (AccountingEntry entry : entries) {
			appendCsvRow(csv, List.of(entry.date().format(DATE), flow(entry), asText(entry.series()),
					asText(entry.number()), asText(entry.accessKey()), asText(entry.counterpartName()),
					asText(entry.counterpartDocument()), asText(entry.cfop()), amount(entry.totalValue()),
					amount(entry.icmsValue()), amount(entry.ipiValue()), amount(entry.pisValue()),
					amount(entry.cofinsValue())));
		}
		return csv.toString();
	}

	private static void appendCsvRow(StringBuilder csv, List<String> cells) {
		csv.append(cells.stream().map(AccountingFileAdapter::quoteIfNeeded)
				.collect(Collectors.joining(String.valueOf(CSV_SEPARATOR)))).append(EOL);
	}

	private static String quoteIfNeeded(String cell) {
		boolean needsQuotes = cell.indexOf(CSV_SEPARATOR) >= 0 || cell.indexOf('"') >= 0 || cell.indexOf('\n') >= 0
				|| cell.indexOf('\r') >= 0;
		return needsQuotes ? '"' + cell.replace("\"", "\"\"") + '"' : cell;
	}

	private static String txt(List<AccountingEntry> entries) {
		StringBuilder txt = new StringBuilder();
		for (AccountingEntry entry : entries) {
			String line = String.join(" ", entry.date().format(DATE), left(flow(entry), FLOW_WIDTH),
					left(singleLine(entry.series()), SERIES_WIDTH), left(singleLine(entry.number()), NUMBER_WIDTH),
					left(singleLine(entry.accessKey()), ACCESS_KEY_WIDTH),
					left(singleLine(entry.counterpartName()), NAME_WIDTH, true),
					left(singleLine(entry.counterpartDocument()), DOCUMENT_WIDTH),
					left(singleLine(entry.cfop()), CFOP_WIDTH), right(amount(entry.totalValue())),
					right(amount(entry.icmsValue())), right(amount(entry.ipiValue())),
					right(amount(entry.pisValue())), right(amount(entry.cofinsValue())));
			txt.append(line.stripTrailing()).append(EOL);
		}
		return txt.toString();
	}

	private static String flow(AccountingEntry entry) {
		return switch (entry.flow()) {
			case ENTRY -> "ENTRADA";
			case EXIT -> "SAIDA";
		};
	}

	/** A text cell of the CSV: empty when absent, and never read as a formula. */
	private static String asText(String value) {
		if (value == null) {
			return "";
		}
		boolean formula = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0;
		return formula ? "'" + value : value;
	}

	/** A text field of the TXT: blank when absent, and kept to the one line its record has. */
	private static String singleLine(String value) {
		return value == null ? "" : value.replaceAll("\\p{Cntrl}", " ").strip();
	}

	private static String left(String value, int width) {
		return String.format("%-" + width + "s", value);
	}

	private static String left(String value, int width, boolean truncate) {
		return left(truncate && value.length() > width ? value.substring(0, width) : value, width);
	}

	private static String right(String value) {
		return String.format("%" + AMOUNT_WIDTH + "s", value);
	}

	private static String amount(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
	}
}
