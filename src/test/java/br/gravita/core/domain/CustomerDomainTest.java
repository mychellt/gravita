package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDomainTest {

	private final DocumentDomain document = DocumentDomain.cpf("111.444.777-35");

	@Test
	void shouldExposeCustomerData() {
		CustomerDomain customer = customerWithStatus(CustomerStatus.ACTIVE);

		assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
		assertThat(customer.getName()).isEqualTo("Maria Silva");
		assertThat(customer.getDocumentDomain()).isEqualTo(document);
	}

	@Test
	void shouldNotAllowBlockingAlreadyBlockedCustomer() {
		CustomerDomain customer = customerWithStatus(CustomerStatus.ACTIVE);
		customer.block();

		assertThatThrownBy(customer::block).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldReactivateBlockedCustomer() {
		CustomerDomain customer = customerWithStatus(CustomerStatus.BLOCKED);

		customer.reactivate();

		assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
	}

	private CustomerDomain customerWithStatus(CustomerStatus status) {
		return CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(document)
				.email("maria@example.com")
				.status(status)
				.build();
	}
}
