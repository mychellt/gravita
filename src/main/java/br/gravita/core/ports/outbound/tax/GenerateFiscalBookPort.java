package br.gravita.core.ports.outbound.tax;

import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;

/**
 * Renders the aggregated books of a period as PDF and TXT (UC-M2-13). It is the period counterpart of
 * {@link GenerateDanfePort}, which renders a single document. Its PDF goes through the shared report renderer
 * ({@code RenderPdfPort}) rather than a renderer of its own, and the TXT is laid out from the same rows, so the two
 * files print the same cells and totals.
 */
public interface GenerateFiscalBookPort {

	FiscalBookFiles generate(LivrosFiscaisBooks books);

	record FiscalBookFiles(byte[] pdf, byte[] txt) {
	}
}
