package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetCustomerCreditStatusAdapterTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@DisplayName("Updates the customer's credit status and balance with the values computed by Finance")
	@Test
	void shouldUpdateStatusAndBalanceWithFinanceComputedValues() {
		SetCustomerCreditStatusAdapter adapter = new SetCustomerCreditStatusAdapter(customerRepositoryPort);
		UUID customerId = UUID.randomUUID();
		CustomerDomain existing = existingCustomer(customerId, CustomerStatus.REGULAR, BigDecimal.ZERO);
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(existing));
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerDomain command = CustomerDomain.builder()
				.id(customerId)
				.currentBalance(new BigDecimal("1500.00"))
				.status(CustomerStatus.BLOCKED)
				.build();

		CustomerDomain updated = adapter.execute(new Context(command));

		assertThat(updated.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
		assertThat(updated.getCurrentBalance()).isEqualByComparingTo("1500.00");
		ArgumentCaptor<CustomerDomain> captor = ArgumentCaptor.forClass(CustomerDomain.class);
		verify(customerRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(customerId);
	}

	@DisplayName("Accepts a delinquent status from Finance as is, without recomputing it")
	@Test
	void shouldAcceptDelinquentStatusWithoutRecomputingIt() {
		SetCustomerCreditStatusAdapter adapter = new SetCustomerCreditStatusAdapter(customerRepositoryPort);
		UUID customerId = UUID.randomUUID();
		CustomerDomain existing = existingCustomer(customerId, CustomerStatus.REGULAR, BigDecimal.ZERO);
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(existing));
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerDomain command = CustomerDomain.builder()
				.id(customerId)
				.currentBalance(new BigDecimal("300.00"))
				.status(CustomerStatus.DELINQUENT)
				.build();

		CustomerDomain updated = adapter.execute(new Context(command));

		assertThat(updated.getStatus()).isEqualTo(CustomerStatus.DELINQUENT);
	}

	@DisplayName("Rejects a credit status update for a customer that does not exist")
	@Test
	void shouldRejectWhenCustomerDoesNotExist() {
		SetCustomerCreditStatusAdapter adapter = new SetCustomerCreditStatusAdapter(customerRepositoryPort);
		UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.empty());

		CustomerDomain command = CustomerDomain.builder()
				.id(customerId)
				.currentBalance(BigDecimal.TEN)
				.status(CustomerStatus.BLOCKED)
				.build();

		assertThatThrownBy(() -> adapter.execute(new Context(command))).isInstanceOf(ResourceNotFoundException.class);

		verify(customerRepositoryPort, never()).save(any());
	}

	private CustomerDomain existingCustomer(UUID id, CustomerStatus status, BigDecimal currentBalance) {
		return CustomerDomain.builder()
				.id(id)
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.status(status)
				.currentBalance(currentBalance)
				.build();
	}
}
