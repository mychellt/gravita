package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record NfseId(UUID value) {

	public NfseId {
		Objects.requireNonNull(value, "NfseId value is required");
	}

	public static NfseId of(UUID value) {
		return new NfseId(value);
	}
}
