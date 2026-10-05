package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of an RPS. An RPS is the pre-conversion record of an {@link NfseDocument} (same aggregate), so it shares
 * that document's UUID.
 */
public record RpsId(UUID value) {

	public RpsId {
		Objects.requireNonNull(value, "RpsId value is required");
	}

	public static RpsId of(final UUID value) {
		return new RpsId(value);
	}

	public NfseId toNfseId() {
		return NfseId.of(value);
	}
}
