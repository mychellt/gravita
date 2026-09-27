package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

public record IssueCorrectionLetterCommand(UUID nfeDocumentId, String text) {

	public IssueCorrectionLetterCommand {
		Objects.requireNonNull(nfeDocumentId, "nfeDocumentId");
	}
}
