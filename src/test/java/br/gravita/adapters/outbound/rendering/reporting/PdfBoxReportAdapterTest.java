package br.gravita.adapters.outbound.rendering.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Column;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.Section;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfBoxReportAdapterTest {

	private static final List<Column> COLUMNS = List.of(new Column("Documento", 3, false),
			new Column("Valor", 1, true));

	private final PdfBoxReportAdapter adapter = new PdfBoxReportAdapter();

	@Test
	void rendersTheTitleHeaderLinesSectionsAndFootersOfAReport() throws IOException {
		byte[] pdf = adapter.render(new PdfReport("Livros Fiscais - 02/2028", List.of("Período: 01/02/2028 a 29/02/2028"),
				List.of(new Section("Livro de Saídas", COLUMNS, List.of(List.of("NFE 1/20", "1.000,00")),
						List.of("Documentos: 1")))));

		String text = textOf(pdf);
		assertThat(text).containsSubsequence("Livros Fiscais - 02/2028", "Período: 01/02/2028 a 29/02/2028",
				"Livro de Saídas", "Documento", "Valor", "NFE 1/20", "1.000,00", "Documentos: 1");
		assertThat(pdf).startsWith("%PDF".getBytes());
	}

	@Test
	void runsALongTableOverSeveralPagesRepeatingItsHeadersAndNumberingThePages() throws IOException {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 1; i <= 150; i++) {
			rows.add(List.of("NFE 1/" + i, "10,00"));
		}

		byte[] pdf = adapter.render(new PdfReport("Livro", List.of(), List.of(new Section("Entradas", COLUMNS, rows,
				List.of("Documentos: 150")))));

		try (PDDocument document = Loader.loadPDF(pdf)) {
			int pages = document.getNumberOfPages();
			assertThat(pages).isGreaterThan(2);
			String text = new PDFTextStripper().getText(document);
			assertThat(text).contains("NFE 1/1", "NFE 1/150", "Documentos: 150", "Página 1 de " + pages,
					"Página " + pages + " de " + pages);
			assertThat(text.split("Documento\\s", -1).length - 1).isEqualTo(pages);
		}
	}

	@Test
	void saysSoWhenASectionHasNoRows() throws IOException {
		byte[] pdf = adapter.render(new PdfReport("Livro", List.of(),
				List.of(new Section("Entradas", COLUMNS, List.of(), List.of("Documentos: 0")))));

		assertThat(textOf(pdf)).contains("Sem documentos no período.", "Documentos: 0");
	}

	@Test
	void cutsATextThatDoesNotFitItsColumnWithAnEllipsis() throws IOException {
		String longText = "Razao Social Muito Longa ".repeat(20);

		byte[] pdf = adapter.render(new PdfReport("Livro", List.of(),
				List.of(new Section("Entradas", COLUMNS, List.of(List.of(longText, "1,00")), List.of()))));

		String text = textOf(pdf);
		assertThat(text).contains("Razao Social").contains("...");
		assertThat(text).doesNotContain(longText.strip());
	}

	@Test
	void printsCharactersTheFontCannotEncodeAsQuestionMarksInsteadOfFailing() throws IOException {
		byte[] pdf = adapter.render(new PdfReport("Livro ☃", List.of("linha\ncom quebra"),
				List.of(new Section("Entradas 中", COLUMNS, List.of(List.of("Açúcar ☃ União", "1,00")),
						List.of()))));

		assertThat(textOf(pdf)).contains("Livro ?", "linha com quebra", "Entradas ?", "Açúcar ? União");
	}

	private String textOf(byte[] pdf) throws IOException {
		try (PDDocument document = Loader.loadPDF(pdf)) {
			return new PDFTextStripper().getText(document);
		}
	}
}
