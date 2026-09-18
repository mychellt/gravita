package br.gravita.system.domain.model;

import br.gravita.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * The endpoint/credentials every adapter implementation (SEFAZ client, Receita Federal
 * client, bank clients, WhatsApp Business API client, e-commerce webhooks, accounting
 * export) reads at call time. The {@code credentialPayload} held here is plaintext from the
 * domain's point of view - encrypting it at rest (doc §11.4) is a persistence-adapter
 * concern, not a domain one.
 */
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

	public static IntegrationCredential register(IntegrationName integrationName, IntegrationEnvironment environment,
			String endpoint, String credentialPayload) {
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

	public void rotate(String endpoint, String credentialPayload) {
		validate(this.integrationName, this.environment, endpoint, credentialPayload);
		this.endpoint = endpoint;
		this.credentialPayload = credentialPayload;
		this.rotatedAt = Instant.now();
	}

	private static void validate(IntegrationName integrationName, IntegrationEnvironment environment, String endpoint,
			String credentialPayload) {
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
