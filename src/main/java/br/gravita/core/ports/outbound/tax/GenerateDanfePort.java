package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.NfeDocument;

/**
 * Renders the DANFE (Documento Auxiliar da NFe) as a PDF (UC-M2-03, AC4):
 * portrait by default, landscape on request, with the issuing company's logo
 * and a barcode of the access key. Net-new for this ticket - no PDF
 * generation existed anywhere in the codebase before it.
 */
public interface GenerateDanfePort {

	byte[] generate(NfeDocument document, Company company, DanfeOrientation orientation);
}
