package br.gravita.masterdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.PixKeyType;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PixKeyTest {

	@Test
	@DisplayName("Accepts a valid CPF key")
	void shouldAcceptValidCpfKey() {
		final PixKey pixKey = PixKey.of("529.982.247-25");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.CPF);
		assertThat(pixKey.value()).isEqualTo("52998224725");
	}

	@Test
	@DisplayName("Rejects a CPF key with an invalid check digit")
	void shouldRejectCpfKeyWithInvalidCheckDigit() {
		assertThatThrownBy(() -> PixKey.of("11111111111"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Accepts a valid CNPJ key")
	void shouldAcceptValidCnpjKey() {
		final PixKey pixKey = PixKey.of("11.222.333/0001-81");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.CNPJ);
		assertThat(pixKey.value()).isEqualTo("11222333000181");
	}

	@Test
	@DisplayName("Accepts an email key")
	void shouldAcceptEmailKey() {
		final PixKey pixKey = PixKey.of("supplier@example.com");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.EMAIL);
	}

	@Test
	@DisplayName("Rejects a malformed email key")
	void shouldRejectMalformedEmailKey() {
		assertThatThrownBy(() -> PixKey.of("not-an-email@"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Accepts a random UUID key")
	void shouldAcceptRandomUuidKey() {
		final PixKey pixKey = PixKey.of("123e4567-e89b-12d3-a456-426614174000");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.RANDOM);
	}

	@Test
	@DisplayName("Rejects an unrecognized key format")
	void shouldRejectUnrecognizedFormat() {
		assertThatThrownBy(() -> PixKey.of("123"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a blank key")
	void shouldRejectBlankKey() {
		assertThatThrownBy(() -> PixKey.of(" "))
				.isInstanceOf(BusinessRuleException.class);
	}
}
