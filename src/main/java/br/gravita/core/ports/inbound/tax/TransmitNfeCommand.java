package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

public record TransmitNfeCommand(UUID nfeDocumentId) {

	public TransmitNfeCommand {
		Objects.requireNonNull(nfeDocumentId, "nfeDocumentId");
	}
}
