package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfeDocument;
import java.util.UUID;

public record NfeDocumentResponse(UUID id, String accessKey, String series, Long number, String status) {

	public static NfeDocumentResponse from(NfeDocument nfeDocument) {
		return new NfeDocumentResponse(nfeDocument.getId().value(), nfeDocument.getAccessKey(),
				nfeDocument.getSeries(), nfeDocument.getNumber(), nfeDocument.getStatus().name());
	}
}
