package br.gravita.adapters.outbound.integration.finance;

import br.gravita.core.domain.finance.BankStatementLine;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.outbound.finance.ImportBankStatementPort;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parses the two statement formats banks export: OFX (both the SGML of 1.x and
 * the XML of 2.x) and CSV. A payload containing an {@code <OFX>} element is OFX,
 * anything else is read as CSV.
 *
 * <p>
 * The CSV needs a header row naming at least a date column ({@code date} /
 * {@code data}) and an amount column ({@code amount} / {@code valor}); a
 * description ({@code description}, {@code descricao}, {@code historico},
 * {@code memo}) and a reference ({@code reference}, {@code id}, {@code documento})
 * column are optional. Fields are separated by {@code ;} or {@code ,} (whichever
 * the header uses), dates are {@code yyyy-MM-dd} or {@code dd/MM/yyyy}, and
 * amounts are signed and use either {@code .} or the Brazilian {@code ,} as the
 * decimal separator.
 */
@Component
class BankStatementParserAdapter implements ImportBankStatementPort {

	private static final Pattern OFX_ROOT = Pattern.compile("<OFX[\\s>]", Pattern.CASE_INSENSITIVE);
	private static final Pattern OFX_TRANSACTION = Pattern.compile("<STMTTRN>(.*?)</STMTTRN>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

	@Override
	public List<BankStatementLine> parse(final String fileContent) {
		if (fileContent == null || fileContent.isBlank()) {
			throw new BusinessRuleException("The bank statement is empty");
		}
		return OFX_ROOT.matcher(fileContent).find() ? parseOfx(fileContent) : parseCsv(fileContent);
	}

	private static List<BankStatementLine> parseOfx(final String content) {
		final List<BankStatementLine> lines = new ArrayList<>();
		final Matcher transactions = OFX_TRANSACTION.matcher(content);
		while (transactions.find()) {
			final int number = lines.size() + 1;
			final String block = transactions.group(1);
			final String posted = ofxValue(block, "DTPOSTED");
			final String amount = ofxValue(block, "TRNAMT");
			if (posted == null || amount == null) {
				throw new BusinessRuleException("OFX transaction " + number + " has no DTPOSTED or TRNAMT");
			}
			final String memo = ofxValue(block, "MEMO");
			final String where = "OFX transaction " + number;
			lines.add(BankStatementLine.unmatched(number, ofxDate(where, posted), amount(where, amount),
					memo != null ? memo : ofxValue(block, "NAME"), ofxValue(block, "FITID")));
		}
		return lines;
	}

	/** The text after {@code <TAG>}, up to the next tag or line break (SGML leaf elements have no closing tag); null if absent or blank. */
	private static String ofxValue(final String block, final String tag) {
		final Matcher matcher = Pattern.compile("<" + tag + ">([^<\\r\\n]*)", Pattern.CASE_INSENSITIVE).matcher(block);
		if (!matcher.find()) {
			return null;
		}
		final String value = unescape(matcher.group(1).trim());
		return value.isEmpty() ? null : value;
	}

	private static String unescape(final String value) {
		return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
				.replace("&amp;", "&");
	}

	/** OFX dates are {@code YYYYMMDD[HHMMSS[.XXX][offset]]}; the bank's posting day is what counts, so the time and zone are dropped. */
	private static LocalDate ofxDate(final String where, final String posted) {
		if (posted.length() < 8) {
			throw new BusinessRuleException(where + " has an invalid DTPOSTED: " + posted);
		}
		try {
			return LocalDate.parse(posted.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
		} catch (final DateTimeParseException e) {
			throw new BusinessRuleException(where + " has an invalid DTPOSTED: " + posted);
		}
	}

	private static List<BankStatementLine> parseCsv(final String content) {
		// Blank rows are dropped, but each keeps its number in the file for reporting.
		final List<String> rows = new ArrayList<>();
		final List<Integer> fileLines = new ArrayList<>();
		int fileLine = 0;
		for (final String row : content.replace("\uFEFF", "").lines().toList()) {
			fileLine++;
			if (!row.isBlank()) {
				rows.add(row);
				fileLines.add(fileLine);
			}
		}
		if (rows.isEmpty()) {
			throw new BusinessRuleException("The bank statement is empty");
		}

		final char delimiter = rows.get(0).indexOf(';') >= 0 ? ';' : ',';
		final List<String> header = split(rows.get(0), delimiter).stream().map(BankStatementParserAdapter::normalise)
				.toList();
		final int dateColumn = column(header, "date", "data");
		final int amountColumn = column(header, "amount", "valor");
		final int descriptionColumn = column(header, "description", "descricao", "historico", "memo");
		final int referenceColumn = column(header, "reference", "id", "documento");
		if (dateColumn < 0 || amountColumn < 0) {
			throw new BusinessRuleException("The CSV statement needs a date and an amount column in its header");
		}

		final List<BankStatementLine> lines = new ArrayList<>();
		for (int i = 1; i < rows.size(); i++) {
			final int number = fileLines.get(i);
			final List<String> fields = split(rows.get(i), delimiter);
			final String date = field(fields, dateColumn);
			final String amount = field(fields, amountColumn);
			if (date == null || amount == null) {
				throw new BusinessRuleException("Line " + number + " has no date or amount");
			}
			final String where = "Line " + number;
			lines.add(BankStatementLine.unmatched(number, csvDate(where, date), amount(where, amount),
					field(fields, descriptionColumn), field(fields, referenceColumn)));
		}
		return lines;
	}

	private static int column(final List<String> header, final String... names) {
		for (final String name : names) {
			final int index = header.indexOf(name);
			if (index >= 0) {
				return index;
			}
		}
		return -1;
	}

	/** A header cell without case, accents and surrounding blanks. */
	private static String normalise(final String cell) {
		return Normalizer.normalize(cell.trim(), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

	private static String field(final List<String> fields, final int column) {
		if (column < 0 || column >= fields.size()) {
			return null;
		}
		final String value = fields.get(column).trim();
		return value.isEmpty() ? null : value;
	}

	/** Splits one row, honouring double-quoted fields (which may hold the delimiter, and {@code ""} for a quote). */
	private static List<String> split(final String row, final char delimiter) {
		final List<String> fields = new ArrayList<>();
		final StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < row.length(); i++) {
			final char c = row.charAt(i);
			if (quoted) {
				if (c == '"' && i + 1 < row.length() && row.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else if (c == '"') {
					quoted = false;
				} else {
					current.append(c);
				}
			} else if (c == '"') {
				quoted = true;
			} else if (c == delimiter) {
				fields.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		fields.add(current.toString());
		return fields;
	}

	private static LocalDate csvDate(final String where, final String value) {
		try {
			return LocalDate.parse(value, value.contains("/") ? BRAZILIAN_DATE : ISO_DATE);
		} catch (final DateTimeParseException e) {
			throw new BusinessRuleException(where + " has an invalid date: " + value);
		}
	}

	/** A signed amount with {@code .} or, when it has a comma, the Brazilian {@code 1.234,56} notation. */
	private static BigDecimal amount(final String where, final String value) {
		String normalised = value.replace("R$", "").replaceAll("\\s", "");
		if (normalised.contains(",")) {
			normalised = normalised.replace(".", "").replace(",", ".");
		}
		try {
			return new BigDecimal(normalised);
		} catch (final NumberFormatException e) {
			throw new BusinessRuleException(where + " has an invalid amount: " + value);
		}
	}
}
