package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationCredential {

	private UUID id;
	private IntegrationName integrationName;
	private IntegrationEnvironment environment;
	private String endpoint;
	private String credentialPayload;
	private Instant rotatedAt;

	public static IntegrationCredential register(final IntegrationName integrationName, final IntegrationEnvironment environment,
			final String endpoint, final String credentialPayload) {
		validate(integrationName, environment, endpoint, credentialPayload);
		return IntegrationCredential.builder()
				.id(UUID.randomUUID())
				.integrationName(integrationName)
				.environment(environment)
				.endpoint(endpoint)
				.credentialPayload(credentialPayload)
				.rotatedAt(Instant.now())
				.build();
	}

	public void rotate(final String endpoint, final String credentialPayload) {
		validate(this.integrationName, this.environment, endpoint, credentialPayload);
		this.endpoint = endpoint;
		this.credentialPayload = credentialPayload;
		this.rotatedAt = Instant.now();
	}

	private static void validate(final IntegrationName integrationName, final IntegrationEnvironment environment, final String endpoint,
			final String credentialPayload) {
		if (integrationName == IntegrationName.SEFAZ && environment == null) {
			throw new BusinessRuleException(
					"SEFAZ credentials must specify an environment (PRODUCTION or HOMOLOGATION)");
		}
		if (endpoint == null || endpoint.isBlank()) {
			throw new BusinessRuleException("Integration endpoint cannot be empty");
		}
		if (credentialPayload == null || credentialPayload.isBlank()) {
			throw new BusinessRuleException("Credential payload cannot be empty");
		}
	}
}
