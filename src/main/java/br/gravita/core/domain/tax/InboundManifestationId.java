package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record InboundManifestationId(UUID value) {

	public InboundManifestationId {
		Objects.requireNonNull(value, "InboundManifestationId value is required");
	}

	public static InboundManifestationId of(UUID value) {
		return new InboundManifestationId(value);
	}
}
