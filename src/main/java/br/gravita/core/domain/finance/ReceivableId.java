package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record ReceivableId(UUID value) {

	public ReceivableId {
		Objects.requireNonNull(value, "ReceivableId value is required");
	}

	public static ReceivableId of(UUID value) {
		return new ReceivableId(value);
	}
}
