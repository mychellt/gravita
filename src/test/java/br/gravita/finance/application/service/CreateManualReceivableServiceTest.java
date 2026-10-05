package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CreateManualReceivableCommand;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.usercases.finance.CreateManualReceivableService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateManualReceivableServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@InjectMocks
	private CreateManualReceivableService service;

	@Test
	@DisplayName("Creates an open manual receivable for a registered customer")
	void createsAnOpenManualReceivableForARegisteredCustomer() {
		final UUID customerId = UUID.randomUUID();
		final LocalDate dueDate = LocalDate.now().plusDays(30);
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));
		when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		final Receivable created = service
				.execute(new CreateManualReceivableCommand(customerId, new BigDecimal("150.00"), dueDate, null));

		assertThat(created.getOrigin()).isEqualTo(ReceivableOrigin.MANUAL);
		assertThat(created.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(created.getCustomerId()).isEqualTo(customerId);
		assertThat(created.getAmount()).isEqualByComparingTo("150.00");
		assertThat(created.getDueDate()).isEqualTo(dueDate);
	}

	@Test
	@DisplayName("Saves the created receivable")
	void savesTheCreatedReceivable() {
		final UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));
		when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(
				new CreateManualReceivableCommand(customerId, BigDecimal.TEN, LocalDate.now().plusDays(1), 3));

		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getInstallments()).isEqualTo(3);
	}

	@Test
	@DisplayName("Rejects an unregistered customer without saving anything")
	void rejectsAnUnregisteredCustomerWithoutSavingAnything() {
		final UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new CreateManualReceivableCommand(customerId, BigDecimal.TEN, LocalDate.now().plusDays(1), null)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a zero amount without saving anything")
	void rejectsAZeroAmountWithoutSavingAnything() {
		final UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));

		assertThatThrownBy(() -> service.execute(
				new CreateManualReceivableCommand(customerId, BigDecimal.ZERO, LocalDate.now().plusDays(1), null)))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}
}
