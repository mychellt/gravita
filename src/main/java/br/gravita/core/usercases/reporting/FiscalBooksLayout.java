package br.gravita.core.usercases.reporting;

import br.gravita.core.ports.inbound.reporting.FiscalBookEntry;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Column;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Section;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Lays the three books out as text rows once, so the PDF and the TXT print the same cells and the same totals. Dates
 * and amounts follow the Brazilian convention ({@code 31/03/2028}, {@code 1.234,56}).
 */
final class FiscalBooksLayout {

	private static final Locale PT_BR = Locale.of("pt", "BR");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
	private static final String NONE = "-";

	private static final List<Column> DOCUMENT_COLUMNS = List.of(new Column("Data", 8, false),
			new Column("Modelo", 6, false), new Column("Série", 5, false), new Column("Número", 8, false),
			new Column("Chave de acesso", 24, false), new Column("Participante", 20, false),
			new Column("CNPJ/CPF", 12, false), new Column("CFOP", 6, false), new Column("Valor total", 10, true),
			new Column("ICMS", 9, true));

	private static final List<Column> ASSESSMENT_COLUMNS = List.of(new Column("Natureza", 8, false),
			new Column("Data", 8, false), new Column("Modelo", 6, false), new Column("Série", 5, false),
			new Column("Número", 8, false), new Column("Participante", 22, false), new Column("CFOP", 6, false),
			new Column("Valor total", 10, true), new Column("ICMS", 9, true));

	private final YearMonth period;
	private final List<FiscalBookEntry> entries;
	private final List<FiscalBookEntry> exits;
	private final List<FiscalBookEntry> icmsAssessment;
	private final BigDecimal icmsDebit;
	private final BigDecimal icmsCredit;

	FiscalBooksLayout(final YearMonth period, final List<FiscalBookEntry> entries, final List<FiscalBookEntry> exits,
			final List<FiscalBookEntry> icmsAssessment, final BigDecimal icmsDebit, final BigDecimal icmsCredit) {
		this.period = period;
		this.entries = entries;
		this.exits = exits;
		this.icmsAssessment = icmsAssessment;
		this.icmsDebit = icmsDebit;
		this.icmsCredit = icmsCredit;
	}

	PdfReport pdfReport() {
		return new PdfReport("Livros Fiscais - " + period.format(MONTH), List.of(periodLine()), sections());
	}

	String txt() {
		final StringBuilder txt = new StringBuilder("LIVROS FISCAIS - ").append(period.format(MONTH)).append('\n')
				.append(periodLine()).append('\n');
		for (final Section section : sections()) {
			txt.append('\n').append(section.heading().toUpperCase(PT_BR)).append('\n');
			appendTable(txt, section);
			section.footerLines().forEach(line -> txt.append(line).append('\n'));
		}
		return txt.toString();
	}

	private List<Section> sections() {
		return List.of(
				new Section("Livro de Entradas", DOCUMENT_COLUMNS, rows(entries, FiscalBooksLayout::documentRow),
						List.of(documentTotals(entries, icmsCredit))),
				new Section("Livro de Saídas", DOCUMENT_COLUMNS, rows(exits, FiscalBooksLayout::documentRow),
						List.of(documentTotals(exits, icmsDebit))),
				new Section("Livro de Apuração do ICMS", ASSESSMENT_COLUMNS,
						rows(icmsAssessment, FiscalBooksLayout::assessmentRow),
						List.of("Débitos (saídas): " + amount(icmsDebit), "Créditos (entradas): " + amount(icmsCredit),
								"Saldo (débitos - créditos): " + amount(icmsDebit.subtract(icmsCredit)))));
	}

	private String periodLine() {
		return "Período: " + period.atDay(1).format(DATE) + " a " + period.atEndOfMonth().format(DATE);
	}

	private static List<List<String>> rows(final List<FiscalBookEntry> book,
			final Function<FiscalBookEntry, List<String>> row) {
		return book.stream().map(row).toList();
	}

	private static List<String> documentRow(final FiscalBookEntry entry) {
		return List.of(date(entry.date()), text(entry.documentModel()), text(entry.series()), text(entry.number()),
				text(entry.accessKey()), text(entry.counterpartName()), text(entry.counterpartDocument()),
				text(entry.cfop()), amount(entry.totalValue()), amount(entry.icmsValue()));
	}

	private static List<String> assessmentRow(final FiscalBookEntry entry) {
		final String nature = switch (entry.flow()) {
			case EXIT -> "Débito";
			case ENTRY -> "Crédito";
		};
		return List.of(nature, date(entry.date()), text(entry.documentModel()), text(entry.series()),
				text(entry.number()), text(entry.counterpartName()), text(entry.cfop()), amount(entry.totalValue()),
				amount(entry.icmsValue()));
	}

	private static String documentTotals(final List<FiscalBookEntry> book, final BigDecimal icms) {
		final BigDecimal total = book.stream().map(FiscalBookEntry::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
		return "Documentos: " + book.size() + " | Valor total: " + amount(total) + " | ICMS: " + amount(icms);
	}

	/** Fixed-width columns sized to their widest cell, amounts flush right. */
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
