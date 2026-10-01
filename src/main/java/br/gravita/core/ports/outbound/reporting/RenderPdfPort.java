package br.gravita.core.ports.outbound.reporting;

import java.util.List;

/**
 * Renders a tabular report as a PDF. It knows nothing of any one report: callers lay their data out as sections of
 * text cells, so every report that needs a PDF (the fiscal books, the export of any report) goes through the one
 * renderer instead of growing one each.
 */
public interface RenderPdfPort {

	byte[] render(PdfReport report);

	/** {@code headerLines} are printed under the title on the first page. */
	record PdfReport(String title, List<String> headerLines, List<Section> sections) {
	}

	/** A titled table. {@code footerLines} are printed under its last row, e.g. its totals. */
	record Section(String heading, List<Column> columns, List<List<String>> rows, List<String> footerLines) {
	}

	/** {@code weight} is the share of the page width the column takes relative to the other columns. */
	record Column(String header, int weight, boolean rightAligned) {
	}
}
