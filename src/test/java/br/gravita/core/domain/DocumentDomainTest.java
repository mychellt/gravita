package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentDomainTest {

	@Test
	void shouldAcceptValidCpf() {
		DocumentDomain documentDomain = DocumentDomain.cpf("111.444.777-35");

		assertThat(documentDomain.number()).isEqualTo("11144477735");
		assertThat(documentDomain.personType()).isEqualTo(PersonType.INDIVIDUAL);
		assertThat(documentDomain.formatted()).isEqualTo("111.444.777-35");
	}

	@Test
	void shouldRejectCpfWithInvalidCheckDigit() {
		assertThatThrownBy(() -> DocumentDomain.cpf("111.444.777-36"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectCpfWithAllDigitsEqual() {
		assertThatThrownBy(() -> DocumentDomain.cpf("111.111.111-11"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAcceptValidCnpj() {
		DocumentDomain documentDomain = DocumentDomain.cnpj("11.222.333/0001-81");

		assertThat(documentDomain.number()).isEqualTo("11222333000181");
		assertThat(documentDomain.personType()).isEqualTo(PersonType.COMPANY);
	}

	@Test
	void shouldRejectCnpjWithInvalidCheckDigit() {
		assertThatThrownBy(() -> DocumentDomain.cnpj("11.222.333/0001-82"))
				.isInstanceOf(BusinessRuleException.class);
	}
}
