package br.gravita.system.adapter.out.persistence;

import br.gravita.shared.PersistenceAdapter;
import br.gravita.system.application.port.out.IntegrationCredentialRepositoryPort;
import br.gravita.system.domain.model.IntegrationCredential;
import br.gravita.system.domain.model.IntegrationEnvironment;
import br.gravita.system.domain.model.IntegrationName;

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
