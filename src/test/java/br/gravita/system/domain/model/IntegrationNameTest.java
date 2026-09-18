package br.gravita.system.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationNameTest {

	@ParameterizedTest
	@ValueSource(strings = {"sefaz", "receita-federal", "viacep-ibge", "bank", "whatsapp-business-api", "ecommerce",
			"accounting", "SEFAZ", "Bank"})
	void shouldResolveEachOfTheSevenValidIntegrations(String code) {
		assertThat(IntegrationName.fromCode(code)).isNotNull();
	}

	@Test
	void shouldRejectUnknownIntegrationName() {
		assertThatThrownBy(() -> IntegrationName.fromCode("stripe"))
				.isInstanceOf(UnknownIntegrationException.class)
				.hasMessageContaining("stripe");
	}

	@Test
	void shouldRejectNullIntegrationName() {
		assertThatThrownBy(() -> IntegrationName.fromCode(null)).isInstanceOf(UnknownIntegrationException.class);
	}
}
