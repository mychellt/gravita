package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.IntegrationCredentialPersistenceMapper;
import br.gravita.core.domain.shared.PersistenceAdapter;
import br.gravita.core.ports.outbound.persistence.system.IntegrationCredentialRepositoryPort;
import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import br.gravita.system.adapter.out.persistence.CredentialCipher;
import br.gravita.system.adapter.out.persistence.IntegrationCredentialJpaRepository;

import java.util.Optional;

@PersistenceAdapter
class IntegrationCredentialRepositoryAdapter implements IntegrationCredentialRepositoryPort {

	private final IntegrationCredentialJpaRepository jpaRepository;
	private final CredentialCipher cipher;
	private final IntegrationCredentialPersistenceMapper mapper = new IntegrationCredentialPersistenceMapper();

	IntegrationCredentialRepositoryAdapter(IntegrationCredentialJpaRepository jpaRepository, CredentialCipher cipher) {
		this.jpaRepository = jpaRepository;
		this.cipher = cipher;
	}

	@Override
	public IntegrationCredential save(IntegrationCredential credential) {
		String encryptedPayload = cipher.encrypt(credential.getCredentialPayload());
		IntegrationCredentialJpaEntity saved = jpaRepository.save(mapper.toEntity(credential, encryptedPayload));
		return mapper.toDomain(saved, credential.getCredentialPayload());
	}

	@Override
	public Optional<IntegrationCredential> findByIntegrationNameAndEnvironment(IntegrationName integrationName,
			IntegrationEnvironment environment) {
		return jpaRepository.findByIntegrationNameAndEnvironment(integrationName, environment)
				.map(entity -> mapper.toDomain(entity, cipher.decrypt(entity.getEncryptedCredentialPayload())));
	}
}
