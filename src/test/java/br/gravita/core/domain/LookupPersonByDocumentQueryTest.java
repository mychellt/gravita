package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.shared.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LookupPersonByDocumentQueryTest {

	@Test
	void shouldBuildDocumentQuery() {
		Document cnpj = Document.cnpj("11444777000161");

		LookupPersonByDocumentQuery query = LookupPersonByDocumentQuery.byDocument(cnpj);

		assertThat(query.isDocumentQuery()).isTrue();
		assertThat(query.document()).isEqualTo(cnpj);
	}

	@Test
	void shouldBuildCepQuery() {
		LookupPersonByDocumentQuery query = LookupPersonByDocumentQuery.byCep("20000000");

		assertThat(query.isDocumentQuery()).isFalse();
		assertThat(query.cep()).isEqualTo("20000000");
	}

	@Test
	void shouldRejectNeitherDocumentNorCep() {
		assertThatThrownBy(() -> new LookupPersonByDocumentQuery(null, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectBothDocumentAndCep() {
		Document cnpj = Document.cnpj("11444777000161");

		assertThatThrownBy(() -> new LookupPersonByDocumentQuery(cnpj, "20000000"))
				.isInstanceOf(BusinessRuleException.class);
	}
}
