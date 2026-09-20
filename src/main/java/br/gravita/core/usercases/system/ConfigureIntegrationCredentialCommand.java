package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.IntegrationEnvironment;

public record ConfigureIntegrationCredentialCommand(
		String integrationName,
		IntegrationEnvironment environment,
		String endpoint,
		String credentialPayload) {
}
