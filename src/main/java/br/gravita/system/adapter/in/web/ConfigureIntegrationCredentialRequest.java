package br.gravita.system.adapter.in.web;

import br.gravita.system.domain.model.IntegrationEnvironment;
import jakarta.validation.constraints.NotBlank;

public record ConfigureIntegrationCredentialRequest(
		IntegrationEnvironment environment,
		@NotBlank String endpoint,
		@NotBlank String credentialPayload) {
}
