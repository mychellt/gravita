package br.gravita.system.domain.model;

import br.gravita.core.domain.system.IntegrationName;
import br.gravita.core.domain.system.UnknownIntegrationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationNameTest {

	@ParameterizedTest
	@DisplayName("Resolves each of the seven valid integrations")
	@ValueSource(strings = {"sefaz", "receita-federal", "viacep-ibge", "bank", "whatsapp-business-api", "ecommerce",
			"accounting", "SEFAZ", "Bank"})
	void shouldResolveEachOfTheSevenValidIntegrations(String code) {
		assertThat(IntegrationName.fromCode(code)).isNotNull();
	}

	@Test
	@DisplayName("Rejects an unknown integration name")
	void shouldRejectUnknownIntegrationName() {
		assertThatThrownBy(() -> IntegrationName.fromCode("stripe"))
				.isInstanceOf(UnknownIntegrationException.class)
				.hasMessageContaining("stripe");
	}

	@Test
	@DisplayName("Rejects a null integration name")
	void shouldRejectNullIntegrationName() {
		assertThatThrownBy(() -> IntegrationName.fromCode(null)).isInstanceOf(UnknownIntegrationException.class);
	}
}
