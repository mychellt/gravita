package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.CustomerNotFoundException;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindCustomerAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	private FindCustomerAdapter adapter;

	private final UserId callerId = UserId.generate();
	private final UUID companyId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		adapter = new FindCustomerAdapter(customerRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@DisplayName("Returns the customer when it exists in the caller's company")
	@Test
	void shouldReturnExistingCustomer() {
		UUID id = UUID.randomUUID();
		CustomerDomain customer = CustomerDomain.builder().id(id).name("Maria Silva").companyId(companyId).build();
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(customerRepositoryPort.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(customer));

		assertThat(adapter.execute(new Context(id).withCaller(callerId))).isSameAs(customer);
	}

	@DisplayName("Throws CustomerNotFoundException when the customer does not exist")
	@Test
	void shouldThrowWhenCustomerDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(customerRepositoryPort.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id).withCaller(callerId)))
				.isInstanceOf(CustomerNotFoundException.class)
				.hasMessage("Customer not found: " + id);
	}

	@DisplayName("Throws CustomerNotFoundException for a customer that belongs to another company")
	@Test
	void shouldThrowNotFoundForCustomerOfAnotherCompany() {
		UUID id = UUID.randomUUID();
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(customerRepositoryPort.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(id).withCaller(callerId)))
				.isInstanceOf(CustomerNotFoundException.class);
		verify(customerRepositoryPort, never()).get(any());
	}

	@DisplayName("Throws CustomerNotFoundException, without touching the repository, when the caller has no company")
	@Test
	void shouldThrowNotFoundWhenCallerHasNoCompany() {
		UUID id = UUID.randomUUID();
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(null).build()));

		assertThatThrownBy(() -> adapter.execute(new Context(id).withCaller(callerId)))
				.isInstanceOf(CustomerNotFoundException.class);
		verify(customerRepositoryPort, never()).findByIdAndCompanyId(any(), any());
		verify(customerRepositoryPort, never()).get(any());
	}

	@DisplayName("Rejects a request without an authenticated caller")
	@Test
	void shouldRejectRequestWithoutCaller() {
		assertThatThrownBy(() -> adapter.execute(new Context(UUID.randomUUID()))).isInstanceOf(UnauthorizedException.class);
	}
}
