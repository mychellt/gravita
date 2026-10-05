package br.gravita.core.domain.masterdata;

import java.util.UUID;

public class DocumentSeriesNotFoundException extends RuntimeException {

	public DocumentSeriesNotFoundException(final UUID companyId, final FiscalDocumentType documentType) {
		super("Document series not found for company " + companyId + " and type " + documentType);
	}
}
