package br.gravita.system.domain.model;

import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationCredentialTest {

	@Test
	@DisplayName("Registers a credential for a non-SEFAZ integration without an environment")
	void shouldRegisterCredentialForNonSefazIntegrationWithoutEnvironment() {
		IntegrationCredential credential = IntegrationCredential.register(IntegrationName.BANK, null,
				"https://bank.example.com/api", "secret-key");

		assertThat(credential.getId()).isNotNull();
		assertThat(credential.getEnvironment()).isNull();
		assertThat(credential.getEndpoint()).isEqualTo("https://bank.example.com/api");
		assertThat(credential.getCredentialPayload()).isEqualTo("secret-key");
		assertThat(credential.getRotatedAt()).isNotNull();
	}

	@Test
	@DisplayName("Rejects a SEFAZ credential without an environment")
	void shouldRejectSefazCredentialWithoutEnvironment() {
		assertThatThrownBy(() -> IntegrationCredential.register(IntegrationName.SEFAZ, null,
				"https://sefaz.example.com", "cert"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("environment");
	}

	@Test
	@DisplayName("Registers a SEFAZ credential when the environment is given")
	void shouldRegisterSefazCredentialWhenEnvironmentIsGiven() {
		IntegrationCredential credential = IntegrationCredential.register(IntegrationName.SEFAZ,
				IntegrationEnvironment.HOMOLOGATION, "https://homologacao.sefaz.example.com", "cert");

		assertThat(credential.getEnvironment()).isEqualTo(IntegrationEnvironment.HOMOLOGATION);
	}

	@Test
	@DisplayName("Rejects a blank endpoint or payload")
	void shouldRejectBlankEndpointOrPayload() {
		assertThatThrownBy(() -> IntegrationCredential.register(IntegrationName.BANK, null, " ", "secret"))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> IntegrationCredential.register(IntegrationName.BANK, null, "https://x", " "))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rotates endpoint and payload in place without changing id or environment")
	void shouldRotateEndpointAndPayloadInPlaceWithoutChangingIdOrEnvironment() {
		IntegrationCredential credential = IntegrationCredential.register(IntegrationName.SEFAZ,
				IntegrationEnvironment.PRODUCTION, "https://sefaz.example.com", "old-cert");
		var originalId = credential.getId();
		Instant originalRotatedAt = credential.getRotatedAt();

		credential.rotate("https://sefaz-v2.example.com", "new-cert");

		assertThat(credential.getId()).isEqualTo(originalId);
		assertThat(credential.getEnvironment()).isEqualTo(IntegrationEnvironment.PRODUCTION);
		assertThat(credential.getEndpoint()).isEqualTo("https://sefaz-v2.example.com");
		assertThat(credential.getCredentialPayload()).isEqualTo("new-cert");
		assertThat(credential.getRotatedAt()).isAfterOrEqualTo(originalRotatedAt);
	}
}
