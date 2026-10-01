package br.gravita.shared;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

	@DisplayName("Accepts a valid CPF")
	@Test
	void shouldAcceptValidCpf() {
		Document document = Document.cpf("111.444.777-35");

		assertThat(document.number()).isEqualTo("11144477735");
		assertThat(document.personType()).isEqualTo(PersonType.INDIVIDUAL);
		assertThat(document.formatted()).isEqualTo("111.444.777-35");
	}

	@DisplayName("Rejects a CPF with an invalid check digit")
	@Test
	void shouldRejectCpfWithInvalidCheckDigit() {
		assertThatThrownBy(() -> Document.cpf("111.444.777-36"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Rejects a CPF whose digits are all the same")
	@Test
	void shouldRejectCpfWithAllDigitsEqual() {
		assertThatThrownBy(() -> Document.cpf("111.111.111-11"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Accepts a valid CNPJ")
	@Test
	void shouldAcceptValidCnpj() {
		Document document = Document.cnpj("11.222.333/0001-81");

		assertThat(document.number()).isEqualTo("11222333000181");
		assertThat(document.personType()).isEqualTo(PersonType.COMPANY);
	}

	@DisplayName("Rejects a CNPJ with an invalid check digit")
	@Test
	void shouldRejectCnpjWithInvalidCheckDigit() {
		assertThatThrownBy(() -> Document.cnpj("11.222.333/0001-82"))
				.isInstanceOf(BusinessRuleException.class);
	}
}
