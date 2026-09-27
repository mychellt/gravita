package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record CancelNfeResponse(UUID id, NfeDocumentStatus status, String cancellationJustification,
		Instant cancelledAt) {

	public static CancelNfeResponse from(NfeDocument document) {
		return new CancelNfeResponse(document.getId().value(), document.getStatus(),
				document.getCancellationJustification(), document.getCancelledAt());
	}
}
