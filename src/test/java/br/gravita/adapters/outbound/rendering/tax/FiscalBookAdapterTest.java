package br.gravita.adapters.outbound.rendering.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.rendering.reporting.PdfBoxReportAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Book;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Flow;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.IcmsAssessment;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Line;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.TaxSummary;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Totals;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.VoidedRange;
import br.gravita.core.ports.outbound.tax.GenerateFiscalBookPort.FiscalBookFiles;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FiscalBookAdapterTest {

	private final FiscalBookAdapter adapter = new FiscalBookAdapter(new PdfBoxReportAdapter());

	@Test
	@DisplayName("Renders the PDF and the TXT with the same books, totals and voided ranges")
	void rendersThePdfAndTheTxtWithTheSameBooksTotalsAndVoidedRanges() throws IOException {
		final FiscalBookFiles files = adapter.generate(books());

		assertThat(files.pdf()).startsWith("%PDF".getBytes());
		final String pdf = textOf(files.pdf());
		final String txt = new String(files.txt(), StandardCharsets.UTF_8);
		// The TXT prints its headings in capitals, the PDF as written: the same words either way.
		for (final String text : List.of(pdf.toLowerCase(Locale.ROOT), txt.toLowerCase(Locale.ROOT))) {
			assertThat(text).containsSubsequence("03/2028", "cnpj: 11222333000181", "inscrição estadual: 123456789",
					"01/03/2028 a 31/03/2028", "livro de entradas", "fornecedor alfa", "1102/1403",
					"documentos: 1 | valor total: 165,00", "livro de saídas", "cliente sa", "5102",
					"documentos: 1 | valor total: 1.015,00", "numeração inutilizada", "101", "110", "10",
					"protocolo-inut-1", "formulários danificados", "faixas: 1 | números inutilizados: 10",
					"livro de apuração do icms", "débito", "crédito", "débitos (saídas): 18,00",
					"créditos (entradas): 27,00", "saldo (débitos - créditos): -9,00", "resumo de tributos", "icms",
					"ipi", "pis", "cofins");
		}
	}

	@Test
	@DisplayName("Prints the TXT as fixed-width columns with amounts flush right")
	void printsTheTxtAsFixedWidthColumnsWithAmountsFlushRight() {
		final String txt = new String(adapter.generate(books()).txt(), StandardCharsets.UTF_8);

		assertThat(txt).startsWith("LIVROS FISCAIS - 03/2028\n");
		assertThat(txt).contains("LIVRO DE ENTRADAS\n", "LIVRO DE SAÍDAS\n", "LIVRO DE APURAÇÃO DO ICMS\n",
				"RESUMO DE TRIBUTOS\n");
		assertThat(txt.lines().filter(line -> line.startsWith("ICMS")).findFirst().orElseThrow())
				.matches("ICMS +18,00 +27,00 +-9,00");
	}

	@Test
	@DisplayName("States that a book has no documents when none exist in the period")
	void saysSoWhereABookHasNoDocumentsInThePeriod() throws IOException {
		final LivrosFiscaisBooks empty = new LivrosFiscaisBooks(CompanyId.of(UUID.randomUUID()), "11222333000181", null,
				YearMonth.of(2028, 3), new Book(List.of(), List.of(), BigDecimal.ZERO),
				new Book(List.of(), List.of(), BigDecimal.ZERO),
				new IcmsAssessment(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
				new TaxSummary(zero(), zero(), zero(), zero()));

		final FiscalBookFiles files = adapter.generate(empty);

		assertThat(new String(files.txt(), StandardCharsets.UTF_8)).contains("(sem registros no período)")
				.contains("Documentos: 0 | Valor total: 0,00").contains("Inscrição estadual: -");
		assertThat(textOf(files.pdf())).contains("Sem documentos no período.");
	}

	private static LivrosFiscaisBooks books() {
		final Line entry = new Line(Flow.ENTRY, LocalDate.of(2028, 3, 1), "1", "100", "3528" + "0".repeat(40),
				"Fornecedor Alfa", "11222333000181", "1102/1403", new BigDecimal("165.00"), new BigDecimal("27.00"),
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		final Line exit = new Line(Flow.EXIT, LocalDate.of(2028, 3, 10), "1", "20", "3528" + "1".repeat(40), "Cliente SA",
				"11222333000181", "5102", new BigDecimal("1015.00"), new BigDecimal("18.00"), new BigDecimal("5.00"),
				new BigDecimal("1.65"), new BigDecimal("7.60"));
		final VoidedRange voided = new VoidedRange("1", 101L, 110L, "formulários danificados", "protocolo-inut-1",
				Instant.parse("2028-03-12T15:00:00Z"));
		return new LivrosFiscaisBooks(CompanyId.of(UUID.randomUUID()), "11222333000181", "123456789",
				YearMonth.of(2028, 3), new Book(List.of(entry), List.of(), new BigDecimal("165.00")),
				new Book(List.of(exit), List.of(voided), new BigDecimal("1015.00")),
				new IcmsAssessment(List.of(exit, entry), new BigDecimal("18.00"), new BigDecimal("27.00"),
						new BigDecimal("-9.00")),
				new TaxSummary(new Totals(new BigDecimal("18.00"), new BigDecimal("27.00"), new BigDecimal("-9.00")),
						zero(), zero(), zero()));
	}

	private static Totals zero() {
		return new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	private static String textOf(final byte[] pdf) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdf)) {
			return new PDFTextStripper().getText(document);
		}
	}
}
