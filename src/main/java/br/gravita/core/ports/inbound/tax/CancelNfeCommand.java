package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

public record CancelNfeCommand(UUID nfeDocumentId, String justification) {

	public CancelNfeCommand {
		Objects.requireNonNull(nfeDocumentId, "nfeDocumentId");
	}
}
