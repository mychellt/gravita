package br.gravita.masterdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.PixKeyType;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

class PixKeyTest {

	@Test
	void shouldAcceptValidCpfKey() {
		PixKey pixKey = PixKey.of("529.982.247-25");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.CPF);
		assertThat(pixKey.value()).isEqualTo("52998224725");
	}

	@Test
	void shouldRejectCpfKeyWithInvalidCheckDigit() {
		assertThatThrownBy(() -> PixKey.of("11111111111"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptValidCnpjKey() {
		PixKey pixKey = PixKey.of("11.222.333/0001-81");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.CNPJ);
		assertThat(pixKey.value()).isEqualTo("11222333000181");
	}

	@Test
	void shouldAcceptEmailKey() {
		PixKey pixKey = PixKey.of("supplier@example.com");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.EMAIL);
	}

	@Test
	void shouldRejectMalformedEmailKey() {
		assertThatThrownBy(() -> PixKey.of("not-an-email@"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptRandomUuidKey() {
		PixKey pixKey = PixKey.of("123e4567-e89b-12d3-a456-426614174000");

		assertThat(pixKey.type()).isEqualTo(PixKeyType.RANDOM);
	}

	@Test
	void shouldRejectUnrecognizedFormat() {
		assertThatThrownBy(() -> PixKey.of("123"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectBlankKey() {
		assertThatThrownBy(() -> PixKey.of(" "))
				.isInstanceOf(BusinessRuleException.class);
	}
}
