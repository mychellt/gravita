package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.system.IntegrationEnvironment;
import jakarta.validation.constraints.NotBlank;

public record ConfigureIntegrationCredentialRequest(
		IntegrationEnvironment environment,
		@NotBlank String endpoint,
		@NotBlank String credentialPayload) {
}
