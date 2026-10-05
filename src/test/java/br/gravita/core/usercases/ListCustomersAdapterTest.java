package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCustomersAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	private ListCustomersAdapter adapter;

	private final UserId callerId = UserId.generate();

	@BeforeEach
	void setUp() {
		adapter = new ListCustomersAdapter(customerRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@DisplayName("Returns only the customers of the caller's company")
	@Test
	void shouldReturnOnlyCustomersOfTheCallersCompany() {
		UUID companyId = UUID.randomUUID();
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		List<CustomerDomain> customers = List.of(
				CustomerDomain.builder().name("Maria Silva").companyId(companyId).build(),
				CustomerDomain.builder().name("João Souza").companyId(companyId).build());
		when(customerRepositoryPort.findAllByCompanyId(companyId)).thenReturn(customers);

		assertThat(adapter.execute(new Context().withCaller(callerId))).isEqualTo(customers);
		verify(customerRepositoryPort, never()).findAll();
	}

	@DisplayName("Returns an empty list, without touching the repository, when the caller has no company")
	@Test
	void shouldReturnEmptyListWhenCallerHasNoCompany() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(null).build()));

		assertThat(adapter.execute(new Context().withCaller(callerId))).isEmpty();
		verify(customerRepositoryPort, never()).findAllByCompanyId(any());
		verify(customerRepositoryPort, never()).findAll();
	}

	@DisplayName("Rejects a request without an authenticated caller")
	@Test
	void shouldRejectRequestWithoutCaller() {
		assertThatThrownBy(() -> adapter.execute(new Context())).isInstanceOf(UnauthorizedException.class);
	}
}
