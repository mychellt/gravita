package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.IntegrationCredentialPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.IntegrationCredentialJpaRepository;
import br.gravita.adapters.outbound.security.CredentialCipher;
import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationCredentialRepositoryAdapterTest {

	@Mock
	private IntegrationCredentialJpaRepository repository;

	@Mock
	private CredentialCipher cipher;

	@Mock
	private IntegrationCredentialPersistenceMapper mapper;

	@InjectMocks
	private IntegrationCredentialRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a credential persisting only the encrypted payload")
	void shouldSaveCredentialPersistingOnlyTheEncryptedPayload() {
		final IntegrationCredential credential = buildCredential("top-secret-api-key");
		final IntegrationCredentialJpaEntity entity = buildEntity();
		when(cipher.encrypt("top-secret-api-key")).thenReturn("encrypted-payload");
		when(mapper.map(credential, "encrypted-payload")).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity, "top-secret-api-key")).thenReturn(credential);

		final IntegrationCredential result = adapter.save(credential);

		assertThat(result).isSameAs(credential);
		verify(cipher).encrypt("top-secret-api-key");
		verify(mapper).map(credential, "encrypted-payload");
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Finds a credential decrypting its payload")
	void shouldFindCredentialWithDecryptedPayload() {
		final IntegrationCredential credential = buildCredential("prod-cert");
		final IntegrationCredentialJpaEntity entity = buildEntity();
		entity.setEncryptedCredentialPayload("encrypted-payload");
		when(repository.findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ, IntegrationEnvironment.PRODUCTION))
				.thenReturn(Optional.of(entity));
		when(cipher.decrypt("encrypted-payload")).thenReturn("prod-cert");
		when(mapper.map(entity, "prod-cert")).thenReturn(credential);

		final Optional<IntegrationCredential> result = adapter.findByIntegrationNameAndEnvironment(
				IntegrationName.SEFAZ, IntegrationEnvironment.PRODUCTION);

		assertThat(result).contains(credential);
		verify(repository).findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ,
				IntegrationEnvironment.PRODUCTION);
	}

	@Test
	@DisplayName("Returns empty when no credential exists for the integration and environment")
	void shouldReturnEmptyWhenNoCredentialExists() {
		when(repository.findByIntegrationNameAndEnvironment(IntegrationName.BANK, null)).thenReturn(Optional.empty());

		assertThat(adapter.findByIntegrationNameAndEnvironment(IntegrationName.BANK, null)).isEmpty();
	}

	private IntegrationCredentialJpaEntity buildEntity() {
		return IntegrationCredentialJpaEntity.builder().id(UUID.randomUUID()).build();
	}

	private IntegrationCredential buildCredential(final String payload) {
		return IntegrationCredential.register(IntegrationName.SEFAZ, IntegrationEnvironment.PRODUCTION,
				"https://nfe.fazenda.example.com", payload);
	}
}
