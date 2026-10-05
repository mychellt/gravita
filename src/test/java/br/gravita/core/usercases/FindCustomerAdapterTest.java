package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.CustomerNotFoundException;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindCustomerAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@DisplayName("Returns the customer when it exists")
	@Test
	void shouldReturnExistingCustomer() {
		FindCustomerAdapter adapter = new FindCustomerAdapter(customerRepositoryPort);
		UUID id = UUID.randomUUID();
		CustomerDomain customer = CustomerDomain.builder().id(id).name("Maria Silva").build();
		when(customerRepositoryPort.get(id)).thenReturn(Optional.of(customer));

		assertThat(adapter.execute(new Context(id))).isSameAs(customer);
	}

	@DisplayName("Throws CustomerNotFoundException when the customer does not exist")
	@Test
	void shouldThrowWhenCustomerDoesNotExist() {
		FindCustomerAdapter adapter = new FindCustomerAdapter(customerRepositoryPort);
		UUID id = UUID.randomUUID();
		when(customerRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id)))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found: " + id);
	}
}
