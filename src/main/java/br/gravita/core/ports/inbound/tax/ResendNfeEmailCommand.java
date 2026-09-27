package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

public record ResendNfeEmailCommand(UUID nfeDocumentId) {

	public ResendNfeEmailCommand {
		Objects.requireNonNull(nfeDocumentId, "nfeDocumentId");
	}
}
