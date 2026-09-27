package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.ManifestationType;
import java.util.Objects;

public record SefazManifestationRequest(String accessKey, ManifestationType type) {

	public SefazManifestationRequest {
		Objects.requireNonNull(accessKey, "accessKey");
		Objects.requireNonNull(type, "type");
	}
}
