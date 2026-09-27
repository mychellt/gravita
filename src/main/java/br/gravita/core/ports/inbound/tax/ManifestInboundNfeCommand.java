package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.ManifestationType;
import java.util.Objects;

public record ManifestInboundNfeCommand(String accessKey, ManifestationType type) {

	public ManifestInboundNfeCommand {
		Objects.requireNonNull(accessKey, "accessKey is required");
		Objects.requireNonNull(type, "type is required");
	}
}
