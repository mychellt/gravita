package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record NfeDocumentId(UUID value) {

	public NfeDocumentId {
		Objects.requireNonNull(value, "NfeDocumentId value is required");
	}

	public static NfeDocumentId of(UUID value) {
		return new NfeDocumentId(value);
	}
}
