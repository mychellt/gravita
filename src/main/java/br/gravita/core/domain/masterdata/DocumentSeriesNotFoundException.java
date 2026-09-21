package br.gravita.core.domain.masterdata;

import java.util.UUID;

public class DocumentSeriesNotFoundException extends RuntimeException {

	public DocumentSeriesNotFoundException(UUID companyId, FiscalDocumentType documentType) {
		super("Document series not found for company " + companyId + " and type " + documentType);
	}
}
