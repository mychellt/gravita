package br.gravita.adapters.outbound.rendering.reporting;

import br.gravita.core.ports.outbound.reporting.RenderPdfPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Renders a {@link PdfReport} on landscape A4 pages with PDFBox: the title once, then each section as a table that
 * runs over as many pages as its rows need, repeating its column headers on each. Text is cut with an ellipsis
 * where it does not fit its column, and characters the built-in Helvetica cannot encode print as {@code ?}, so no
 * input can fail the render.
 */
@Component
public class PdfBoxReportAdapter implements RenderPdfPort {

	private static final PDRectangle PAGE_SIZE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
	private static final float MARGIN = 30f;
	private static final float CELL_PADDING = 2f;
	private static final float TITLE_SIZE = 14f;
	private static final float HEADING_SIZE = 11f;
	private static final float BODY_SIZE = 7f;
	private static final float ROW_HEIGHT = 11f;
	private static final String ELLIPSIS = "...";

	private final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

	@Override
	public byte[] render(PdfReport report) {
		try (PDDocument pdf = new PDDocument()) {
			Pages pages = new Pages(pdf);
			try {
				pages.line(bold, TITLE_SIZE, report.title());
				pages.skip(4);
				for (String line : report.headerLines()) {
					pages.line(font, BODY_SIZE + 2, line);
				}
				for (Section section : report.sections()) {
					drawSection(pages, section);
				}
			} finally {
				pages.close();
			}
			addPageNumbers(pdf);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			pdf.save(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to render the PDF report " + report.title(), e);
		}
	}

	private void drawSection(Pages pages, Section section) throws IOException {
		pages.skip(10);
		// Keep the heading with the table header and a first row.
		pages.ensureSpace(HEADING_SIZE + 6 + 3 * ROW_HEIGHT);
		pages.line(bold, HEADING_SIZE, section.heading());
		pages.skip(2);
		drawRow(pages, section, headers(section), bold, true);
		if (section.rows().isEmpty()) {
			pages.row(font, BODY_SIZE, List.of(new Cell(MARGIN + CELL_PADDING, "Sem documentos no período.")));
		}
		for (List<String> row : section.rows()) {
			if (pages.remaining() < ROW_HEIGHT) {
				pages.newPage();
				drawRow(pages, section, headers(section), bold, true);
			}
			drawRow(pages, section, row, font, false);
		}
		pages.skip(4);
		for (String footer : section.footerLines()) {
			pages.line(bold, BODY_SIZE + 1, footer);
		}
	}

	private List<String> headers(Section section) {
		return section.columns().stream().map(Column::header).toList();
	}

	private void drawRow(Pages pages, Section section, List<String> cells, PDFont rowFont, boolean ruled)
			throws IOException {
		float usable = PAGE_SIZE.getWidth() - 2 * MARGIN;
		int totalWeight = section.columns().stream().mapToInt(Column::weight).sum();
		List<Cell> placed = new ArrayList<>();
		float x = MARGIN;
		for (int i = 0; i < section.columns().size(); i++) {
			Column column = section.columns().get(i);
			float width = usable * column.weight() / totalWeight;
			String text = fit(cells.get(i), rowFont, BODY_SIZE, width - 2 * CELL_PADDING);
			float textX = column.rightAligned() ? x + width - CELL_PADDING - widthOf(text, rowFont, BODY_SIZE)
					: x + CELL_PADDING;
			placed.add(new Cell(textX, text));
			x += width;
		}
		pages.row(rowFont, BODY_SIZE, placed);
		if (ruled) {
			pages.rule();
		}
	}

	private String fit(String value, PDFont textFont, float size, float width) {
		String text = encodable(value, textFont);
		if (widthOf(text, textFont, size) <= width) {
			return text;
		}
		while (!text.isEmpty() && widthOf(text + ELLIPSIS, textFont, size) > width) {
			text = text.substring(0, text.length() - 1);
		}
		return text + ELLIPSIS;
	}

	private static String encodable(String value, PDFont textFont) {
		StringBuilder text = new StringBuilder();
		for (char c : (value == null ? "" : value).toCharArray()) {
			String character = Character.isWhitespace(c) ? " " : String.valueOf(c);
			try {
				textFont.encode(character);
				text.append(character);
			} catch (IllegalArgumentException | IOException e) {
				text.append('?');
			}
		}
		return text.toString();
	}

	private float widthOf(String text, PDFont textFont, float size) {
		try {
			return textFont.getStringWidth(text) / 1000f * size;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void addPageNumbers(PDDocument pdf) throws IOException {
		int total = pdf.getNumberOfPages();
		for (int i = 0; i < total; i++) {
			try (PDPageContentStream content = new PDPageContentStream(pdf, pdf.getPage(i), AppendMode.APPEND, true)) {
				String label = "Página " + (i + 1) + " de " + total;
				content.beginText();
				content.setFont(font, BODY_SIZE);
				content.newLineAtOffset(PAGE_SIZE.getWidth() - MARGIN - widthOf(label, font, BODY_SIZE), MARGIN / 2);
				content.showText(label);
				content.endText();
			}
		}
	}

	private record Cell(float x, String text) {
	}

	/** The page being written to and where the next line goes; opens a new page when one fills up. */
	private static final class Pages {

		private final PDDocument pdf;
		private PDPageContentStream content;
		private float y;

		Pages(PDDocument pdf) throws IOException {
			this.pdf = pdf;
			newPage();
		}

		void newPage() throws IOException {
			close();
			PDPage page = new PDPage(PAGE_SIZE);
			pdf.addPage(page);
			content = new PDPageContentStream(pdf, page);
			y = PAGE_SIZE.getHeight() - MARGIN;
		}

		float remaining() {
			return y - MARGIN;
		}

		void ensureSpace(float height) throws IOException {
			if (remaining() < height) {
				newPage();
			}
		}

		void skip(float height) {
			y -= height;
		}

		/** A free-standing line of text; text longer than the page is not wrapped. */
		void line(PDFont lineFont, float size, String text) throws IOException {
			ensureSpace(size + 4);
			y -= size;
			show(lineFont, size, MARGIN, text);
			y -= 4;
		}

		void row(PDFont rowFont, float size, List<Cell> cells) throws IOException {
			ensureSpace(ROW_HEIGHT);
			y -= size;
			for (Cell cell : cells) {
				show(rowFont, size, cell.x(), cell.text());
			}
			y -= ROW_HEIGHT - size;
		}

		void rule() throws IOException {
			content.moveTo(MARGIN, y + 2);
			content.lineTo(PAGE_SIZE.getWidth() - MARGIN, y + 2);
			content.stroke();
		}

		private void show(PDFont textFont, float size, float x, String text) throws IOException {
			content.beginText();
			content.setFont(textFont, size);
			content.newLineAtOffset(x, y);
			content.showText(encodable(text, textFont));
			content.endText();
		}

		void close() throws IOException {
			if (content != null) {
				content.close();
				content = null;
			}
		}
	}
}
