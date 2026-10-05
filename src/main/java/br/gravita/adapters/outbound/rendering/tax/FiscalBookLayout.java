package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Line;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Totals;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.VoidedRange;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Column;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Section;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Lays the books out as text rows, for both the PDF and the TXT. Dates and amounts follow the Brazilian convention
 * ({@code 31/03/2028}, {@code 1.234,56}); an absent value prints as {@code -}.
 */
final class FiscalBookLayout {

	private static final Locale PT_BR = Locale.of("pt", "BR");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
	private static final DateTimeFormatter INSTANT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
			.withZone(ZoneId.systemDefault());
	private static final String NONE = "-";
	private static final String NO_ROWS = "(sem registros no período)";

	/** Weights leave the 44-digit access key and a joined CFOP such as {@code 1102/1403} room to print untruncated. */
	private static final List<Column> DOCUMENT_COLUMNS = List.of(new Column("Data", 6, false),
			new Column("Série", 4, false), new Column("Número", 5, false), new Column("Chave de acesso", 25, false),
			new Column("Participante", 14, false), new Column("CNPJ/CPF", 9, false), new Column("CFOP", 6, false),
			new Column("Valor total", 8, true), new Column("ICMS", 6, true), new Column("IPI", 6, true),
			new Column("PIS", 6, true), new Column("COFINS", 6, true));

	private static final List<Column> VOIDED_COLUMNS = List.of(new Column("Série", 4, false),
			new Column("Número inicial", 8, false), new Column("Número final", 8, false),
			new Column("Qtde", 4, true), new Column("Inutilizada em", 10, false), new Column("Protocolo", 14, false),
			new Column("Justificativa", 40, false));

	private static final List<Column> ASSESSMENT_COLUMNS = List.of(new Column("Natureza", 8, false),
			new Column("Data", 8, false), new Column("Série", 5, false), new Column("Número", 8, false),
			new Column("Participante", 22, false), new Column("CFOP", 6, false), new Column("Valor total", 10, true),
			new Column("ICMS", 9, true));

	private static final List<Column> SUMMARY_COLUMNS = List.of(new Column("Tributo", 10, false),
			new Column("Saídas (débito)", 14, true), new Column("Entradas (crédito)", 14, true),
			new Column("Saldo (débito - crédito)", 16, true));

	private FiscalBookLayout() {
	}

	static PdfReport layout(final LivrosFiscaisBooks books) {
		final YearMonth period = books.period();
		final List<String> header = List.of(
				"CNPJ: " + text(books.companyCnpj()) + " | Inscrição estadual: " + text(books.companyIe()),
				"Período: " + period.atDay(1).format(DATE) + " a " + period.atEndOfMonth().format(DATE));
		return new PdfReport("Livros Fiscais - " + period.format(MONTH), header, List.of(entrySection(books),
				exitSection(books), voidedSection(books), assessmentSection(books), summarySection(books)));
	}

	/** Prints a report as fixed-width text: columns sized to their widest cell, amounts flush right. */
	static String toText(final PdfReport report) {
		final StringBuilder txt = new StringBuilder(report.title().toUpperCase(PT_BR)).append('\n');
		report.headerLines().forEach(line -> txt.append(line).append('\n'));
		for (final Section section : report.sections()) {
			txt.append('\n').append(section.heading().toUpperCase(PT_BR)).append('\n');
			appendTable(txt, section);
			section.footerLines().forEach(line -> txt.append(line).append('\n'));
		}
		return txt.toString();
	}

	private static Section entrySection(final LivrosFiscaisBooks books) {
		return new Section("Livro de Entradas", DOCUMENT_COLUMNS, rows(books.entryBook().lines(), FiscalBookLayout::documentRow),
				List.of(documentTotals(books.entryBook().lines().size(), books.entryBook().totalValue())));
	}

	private static Section exitSection(final LivrosFiscaisBooks books) {
		return new Section("Livro de Saídas", DOCUMENT_COLUMNS, rows(books.exitBook().lines(), FiscalBookLayout::documentRow),
				List.of(documentTotals(books.exitBook().lines().size(), books.exitBook().totalValue())));
	}

	/** The voided ranges, so a gap in the exit book's numbering is explained by a line here. */
	private static Section voidedSection(final LivrosFiscaisBooks books) {
		final List<VoidedRange> ranges = books.exitBook().voidedRanges();
		final long numbers = ranges.stream().mapToLong(VoidedRange::quantity).sum();
		return new Section("Livro de Saídas - Numeração inutilizada", VOIDED_COLUMNS,
				rows(ranges, FiscalBookLayout::voidedRow),
				List.of("Faixas: " + ranges.size() + " | Números inutilizados: " + numbers));
	}

	private static Section assessmentSection(final LivrosFiscaisBooks books) {
		final LivrosFiscaisBooks.IcmsAssessment assessment = books.icmsAssessmentBook();
		return new Section("Livro de Apuração do ICMS", ASSESSMENT_COLUMNS,
				rows(assessment.lines(), FiscalBookLayout::assessmentRow),
				List.of("Débitos (saídas): " + amount(assessment.debit()),
						"Créditos (entradas): " + amount(assessment.credit()),
						"Saldo (débitos - créditos): " + amount(assessment.balance())));
	}

	private static Section summarySection(final LivrosFiscaisBooks books) {
		final LivrosFiscaisBooks.TaxSummary summary = books.taxSummary();
		return new Section("Resumo de Tributos", SUMMARY_COLUMNS,
				List.of(summaryRow("ICMS", summary.icms()), summaryRow("IPI", summary.ipi()),
						summaryRow("PIS", summary.pis()), summaryRow("COFINS", summary.cofins())),
				List.of());
	}

	private static <T> List<List<String>> rows(final List<T> items, final Function<T, List<String>> row) {
		return items.stream().map(row).toList();
	}

	private static List<String> documentRow(final Line line) {
		return List.of(date(line.date()), text(line.series()), text(line.number()), text(line.accessKey()),
				text(line.counterpartName()), text(line.counterpartDocument()), text(line.cfop()),
				amount(line.totalValue()), amount(line.icmsValue()), amount(line.ipiValue()),
				amount(line.pisValue()), amount(line.cofinsValue()));
	}

	private static List<String> voidedRow(final VoidedRange range) {
		return List.of(text(range.series()), String.valueOf(range.startNumber()), String.valueOf(range.endNumber()),
				String.valueOf(range.quantity()), INSTANT.format(range.voidedAt()), text(range.sefazProtocol()),
				text(range.justification()));
	}

	private static List<String> assessmentRow(final Line line) {
		final String nature = switch (line.flow()) {
			case EXIT -> "Débito";
			case ENTRY -> "Crédito";
		};
		return List.of(nature, date(line.date()), text(line.series()), text(line.number()),
				text(line.counterpartName()), text(line.cfop()), amount(line.totalValue()), amount(line.icmsValue()));
	}

	private static List<String> summaryRow(final String tax, final Totals totals) {
		return List.of(tax, amount(totals.onExits()), amount(totals.onEntries()), amount(totals.balance()));
	}

	private static String documentTotals(final int documents, final BigDecimal totalValue) {
		return "Documentos: " + documents + " | Valor total: " + amount(totalValue);
	}

	private static void appendTable(final StringBuilder txt, final Section section) {
		final List<Column> columns = section.columns();
		final int[] widths = new int[columns.size()];
		for (int i = 0; i < widths.length; i++) {
			widths[i] = columns.get(i).header().length();
			for (final List<String> row : section.rows()) {
				widths[i] = Math.max(widths[i], row.get(i).length());
			}
		}
		appendRow(txt, columns, widths, columns.stream().map(Column::header).toList());
		section.rows().forEach(row -> appendRow(txt, columns, widths, row));
		if (section.rows().isEmpty()) {
			txt.append(NO_ROWS).append('\n');
		}
	}

	private static void appendRow(final StringBuilder txt, final List<Column> columns, final int[] widths, final List<String> cells) {
		final StringBuilder line = new StringBuilder();
		for (int i = 0; i < widths.length; i++) {
			final String format = "%" + (columns.get(i).rightAligned() ? "" : "-") + widths[i] + "s";
			line.append(i == 0 ? "" : "  ").append(String.format(format, cells.get(i)));
		}
		txt.append(line.toString().stripTrailing()).append('\n');
	}

	private static String date(final LocalDate date) {
		return date.format(DATE);
	}

	private static String text(final String value) {
		return value == null || value.isBlank() ? NONE : value;
	}

	private static String amount(final BigDecimal value) {
		final NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
		format.setMinimumFractionDigits(2);
		format.setMaximumFractionDigits(2);
		return format.format(value);
	}
}
