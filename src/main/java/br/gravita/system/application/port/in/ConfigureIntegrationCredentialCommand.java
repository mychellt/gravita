package br.gravita.system.application.port.in;

import br.gravita.system.domain.model.IntegrationEnvironment;

public record ConfigureIntegrationCredentialCommand(
		String integrationName,
		IntegrationEnvironment environment,
		String endpoint,
		String credentialPayload) {
}
