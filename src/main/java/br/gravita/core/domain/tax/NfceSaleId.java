package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record NfceSaleId(UUID value) {

	public NfceSaleId {
		Objects.requireNonNull(value, "NfceSaleId value is required");
	}

	public static NfceSaleId of(UUID value) {
		return new NfceSaleId(value);
	}
}
