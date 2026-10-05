package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import java.util.UUID;

public record IssueNfeResponse(UUID id, String accessKey, String documentSeries, Long documentNumber,
		NfeDocumentStatus status) {

	public static IssueNfeResponse from(final NfeDocument document) {
		return new IssueNfeResponse(document.getId().value(), document.getAccessKey(), document.getDocumentSeries(),
				document.getDocumentNumber(), document.getStatus());
	}
}
