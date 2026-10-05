package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCustomersAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@DisplayName("Returns every customer held by the repository")
	@Test
	void shouldReturnAllCustomers() {
		ListCustomersAdapter adapter = new ListCustomersAdapter(customerRepositoryPort);
		List<CustomerDomain> customers = List.of(
				CustomerDomain.builder().name("Maria Silva").build(),
				CustomerDomain.builder().name("João Souza").build());
		when(customerRepositoryPort.findAll()).thenReturn(customers);

		assertThat(adapter.execute(new Context())).isEqualTo(customers);
	}
}
