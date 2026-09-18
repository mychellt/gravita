package br.gravita.core.usercases;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.DocumentDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.persistence.CustomerRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerRegistrationAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Test
	void shouldAssignIdSetRegularStatusAndZeroBalanceOnRegistration() {
		CustomerRegistrationAdapter adapter = new CustomerRegistrationAdapter(customerRepositoryPort);
		CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(DocumentDomain.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.build();
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerDomain created = adapter.execute(new Context(customer));

		assertThat(created.getId()).isNotNull();
		assertThat(created.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(created.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
		ArgumentCaptor<CustomerDomain> captor = ArgumentCaptor.forClass(CustomerDomain.class);
		verify(customerRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(created.getId());
	}

	@Test
	void shouldKeepExistingIdWhenAlreadySet() {
		CustomerRegistrationAdapter adapter = new CustomerRegistrationAdapter(customerRepositoryPort);
		UUID existingId = UUID.randomUUID();
		CustomerDomain customer = CustomerDomain.builder()
				.id(existingId)
				.name("Maria Silva")
				.documentDomain(DocumentDomain.cpf("111.444.777-35"))
				.addresses(List.of(billingAddress()))
				.build();
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerDomain created = adapter.execute(new Context(customer));

		assertThat(created.getId()).isEqualTo(existingId);
	}

	@Test
	void shouldRejectRegistrationWithoutAddressBeforeSaving() {
		CustomerRegistrationAdapter adapter = new CustomerRegistrationAdapter(customerRepositoryPort);
		CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(DocumentDomain.cpf("111.444.777-35"))
				.addresses(List.of())
				.build();

		assertThatThrownBy(() -> adapter.execute(new Context(customer))).isInstanceOf(BusinessRuleException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	@Test
	void shouldRejectCompanyCustomerMissingFiscalDataBeforeSaving() {
		CustomerRegistrationAdapter adapter = new CustomerRegistrationAdapter(customerRepositoryPort);
		CustomerDomain customer = CustomerDomain.builder()
				.name("Acme LTDA")
				.documentDomain(DocumentDomain.cnpj("11.222.333/0001-81"))
				.addresses(List.of(billingAddress()))
				.build();

		assertThatThrownBy(() -> adapter.execute(new Context(customer))).isInstanceOf(BusinessRuleException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	private AddressDomain billingAddress() {
		return AddressDomain.builder().type(AddressType.BILLING).street("Rua A").neighborhood("Centro")
				.city("São Paulo").state("SP").zipCode("01000-000").isDefault(true).build();
	}
}
