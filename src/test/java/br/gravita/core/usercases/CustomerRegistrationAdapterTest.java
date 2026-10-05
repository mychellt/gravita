package br.gravita.core.usercases;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerRegistrationAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	private final UserId callerId = UserId.generate();
	private final UUID companyId = UUID.randomUUID();

	@BeforeEach
	void callerBelongsToCompany() {
		lenient().when(userRepositoryPort.findById(callerId))
				.thenReturn(Optional.of(User.builder().companyId(companyId).build()));
	}

	private CustomerRegistrationAdapter adapter() {
		return new CustomerRegistrationAdapter(customerRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	private Context asCaller(final CustomerDomain customer) {
		return new Context(customer).withCaller(callerId);
	}

	@DisplayName("Registering a customer assigns an id, sets regular status and a zero balance")
	@Test
	void shouldAssignIdSetRegularStatusAndZeroBalanceOnRegistration() {
		final CustomerRegistrationAdapter adapter = adapter();
		final CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.build();
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final CustomerDomain created = adapter.execute(asCaller(customer));

		assertThat(created.getId()).isNotNull();
		assertThat(created.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(created.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
		final ArgumentCaptor<CustomerDomain> captor = ArgumentCaptor.forClass(CustomerDomain.class);
		verify(customerRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(created.getId());
	}

	@DisplayName("Registering a customer that already has an id keeps that id")
	@Test
	void shouldKeepExistingIdWhenAlreadySet() {
		final CustomerRegistrationAdapter adapter = adapter();
		final UUID existingId = UUID.randomUUID();
		final CustomerDomain customer = CustomerDomain.builder()
				.id(existingId)
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.build();
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final CustomerDomain created = adapter.execute(asCaller(customer));

		assertThat(created.getId()).isEqualTo(existingId);
	}

	@DisplayName("Rejects a customer without an address before anything is saved")
	@Test
	void shouldRejectRegistrationWithoutAddressBeforeSaving() {
		final CustomerRegistrationAdapter adapter = adapter();
		final CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.addresses(List.of())
				.build();

		assertThatThrownBy(() -> adapter.execute(asCaller(customer))).isInstanceOf(BusinessRuleException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	@DisplayName("Rejects a company customer missing fiscal data before anything is saved")
	@Test
	void shouldRejectCompanyCustomerMissingFiscalDataBeforeSaving() {
		final CustomerRegistrationAdapter adapter = adapter();
		final CustomerDomain customer = CustomerDomain.builder()
				.name("Acme LTDA")
				.documentDomain(Document.cnpj("11.222.333/0001-81"))
				.addresses(List.of(billingAddress()))
				.build();

		assertThatThrownBy(() -> adapter.execute(asCaller(customer))).isInstanceOf(BusinessRuleException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	@DisplayName("Stamps the new customer with the caller's company, ignoring any company already on the payload")
	@Test
	void shouldStampCustomerWithTheCallersCompany() {
		final CustomerRegistrationAdapter adapter = adapter();
		final CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.companyId(UUID.randomUUID())
				.build();
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final CustomerDomain created = adapter.execute(asCaller(customer));

		assertThat(created.getCompanyId()).isEqualTo(companyId);
		final ArgumentCaptor<CustomerDomain> captor = ArgumentCaptor.forClass(CustomerDomain.class);
		verify(customerRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getCompanyId()).isEqualTo(companyId);
	}

	@DisplayName("Rejects the registration, saving nothing, when the caller has no company")
	@Test
	void shouldRejectRegistrationWhenCallerHasNoCompany() {
		final UserId companylessCaller = UserId.generate();
		when(userRepositoryPort.findById(companylessCaller)).thenReturn(Optional.of(User.builder().companyId(null).build()));
		final CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.build();

		assertThatThrownBy(() -> adapter().execute(new Context(customer).withCaller(companylessCaller)))
				.isInstanceOf(ForbiddenException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	@DisplayName("Rejects the registration, saving nothing, when there is no authenticated caller")
	@Test
	void shouldRejectRegistrationWithoutCaller() {
		final CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.build();

		assertThatThrownBy(() -> adapter().execute(new Context(customer))).isInstanceOf(UnauthorizedException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	private AddressDomain billingAddress() {
		return AddressDomain.builder().type(AddressType.BILLING).street("Rua A").neighborhood("Centro")
				.city("São Paulo").state("SP").zipCode("01000-000").isDefault(true).build();
	}
}
