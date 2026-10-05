package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.IntegrationCredentialPersistenceMapper;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.IntegrationCredentialRepositoryPort;
import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import br.gravita.adapters.outbound.security.CredentialCipher;
import br.gravita.adapters.outbound.persistence.repositories.tax.IntegrationCredentialJpaRepository;

import java.util.Optional;

@PersistenceAdapter
class IntegrationCredentialRepositoryAdapter implements IntegrationCredentialRepositoryPort {

	private final IntegrationCredentialJpaRepository jpaRepository;
	private final CredentialCipher cipher;
	private final IntegrationCredentialPersistenceMapper mapper;

	IntegrationCredentialRepositoryAdapter(final IntegrationCredentialJpaRepository jpaRepository, final CredentialCipher cipher,
			final IntegrationCredentialPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.cipher = cipher;
		this.mapper = mapper;
	}

	@Override
	public IntegrationCredential save(final IntegrationCredential credential) {
		final String encryptedPayload = cipher.encrypt(credential.getCredentialPayload());
		final IntegrationCredentialJpaEntity saved = jpaRepository.save(mapper.map(credential, encryptedPayload));
		return mapper.map(saved, credential.getCredentialPayload());
	}

	@Override
	public Optional<IntegrationCredential> findByIntegrationNameAndEnvironment(final IntegrationName integrationName,
			final IntegrationEnvironment environment) {
		return jpaRepository.findByIntegrationNameAndEnvironment(integrationName, environment)
				.map(entity -> mapper.map(entity, cipher.decrypt(entity.getEncryptedCredentialPayload())));
	}
}
