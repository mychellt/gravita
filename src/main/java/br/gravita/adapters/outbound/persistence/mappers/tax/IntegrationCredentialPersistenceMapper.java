package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;

class IntegrationCredentialPersistenceMapper {

	IntegrationCredentialJpaEntity toEntity(IntegrationCredential domain, String encryptedCredentialPayload) {
		return IntegrationCredentialJpaEntity.builder()
				.id(domain.getId())
				.integrationName(domain.getIntegrationName())
				.environment(domain.getEnvironment())
				.endpoint(domain.getEndpoint())
				.encryptedCredentialPayload(encryptedCredentialPayload)
				.rotatedAt(domain.getRotatedAt())
				.build();
	}

	IntegrationCredential toDomain(IntegrationCredentialJpaEntity entity, String decryptedCredentialPayload) {
		return IntegrationCredential.builder()
				.id(entity.getId())
				.integrationName(entity.getIntegrationName())
				.environment(entity.getEnvironment())
				.endpoint(entity.getEndpoint())
				.credentialPayload(decryptedCredentialPayload)
				.rotatedAt(entity.getRotatedAt())
				.build();
	}
}
