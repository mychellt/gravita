package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.IntegrationCredentialPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.IntegrationCredentialJpaRepository;
import br.gravita.adapters.outbound.security.CredentialCipher;
import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({IntegrationCredentialRepositoryAdapter.class, CredentialCipher.class, IntegrationCredentialPersistenceMapperImpl.class})
class IntegrationCredentialRepositoryAdapterTest {

	@Autowired
	private IntegrationCredentialRepositoryAdapter repositoryAdapter;

	@Autowired
	private IntegrationCredentialJpaRepository jpaRepository;

	@Test
	void shouldSaveAndRetrieveCredentialWithDecryptedPayload() {
		IntegrationCredential credential = IntegrationCredential.register(IntegrationName.BANK, null,
				"https://bank.example.com/api", "top-secret-api-key");

		repositoryAdapter.save(credential);

		Optional<IntegrationCredential> found = repositoryAdapter.findByIntegrationNameAndEnvironment(
				IntegrationName.BANK, null);
		assertThat(found).isPresent();
		assertThat(found.get().getCredentialPayload()).isEqualTo("top-secret-api-key");
		assertThat(found.get().getEndpoint()).isEqualTo("https://bank.example.com/api");
	}

	@Test
	void shouldNeverPersistThePlaintextPayload() {
		IntegrationCredential credential = IntegrationCredential.register(IntegrationName.WHATSAPP_BUSINESS_API, null,
				"https://graph.facebook.com", "plain-token-value");

		repositoryAdapter.save(credential);

		IntegrationCredentialJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(stored.getEncryptedCredentialPayload()).doesNotContain("plain-token-value");
	}

	@Test
	void shouldKeepProductionAndHomologationSefazCredentialsIndependent() {
		IntegrationCredential production = IntegrationCredential.register(IntegrationName.SEFAZ,
				IntegrationEnvironment.PRODUCTION, "https://nfe.fazenda.example.com", "prod-cert");
		IntegrationCredential homologation = IntegrationCredential.register(IntegrationName.SEFAZ,
				IntegrationEnvironment.HOMOLOGATION, "https://homologacao.nfe.fazenda.example.com", "homolog-cert");

		repositoryAdapter.save(production);
		repositoryAdapter.save(homologation);

		assertThat(repositoryAdapter.findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ,
				IntegrationEnvironment.PRODUCTION)).get().extracting(IntegrationCredential::getCredentialPayload)
				.isEqualTo("prod-cert");
		assertThat(repositoryAdapter.findByIntegrationNameAndEnvironment(IntegrationName.SEFAZ,
				IntegrationEnvironment.HOMOLOGATION)).get().extracting(IntegrationCredential::getCredentialPayload)
				.isEqualTo("homolog-cert");
	}

	@Test
	void rotatingAndSavingAgainImmediatelyReflectsTheNewValueOnNextRead() {
		IntegrationCredential credential = IntegrationCredential.register(IntegrationName.ECOMMERCE, null,
				"https://old.example.com", "old-token");
		repositoryAdapter.save(credential);

		credential.rotate("https://new.example.com", "new-token");
		repositoryAdapter.save(credential);

		Optional<IntegrationCredential> found = repositoryAdapter.findByIntegrationNameAndEnvironment(
				IntegrationName.ECOMMERCE, null);
		assertThat(found).isPresent();
		assertThat(found.get().getCredentialPayload()).isEqualTo("new-token");
		assertThat(found.get().getEndpoint()).isEqualTo("https://new.example.com");
	}
}
