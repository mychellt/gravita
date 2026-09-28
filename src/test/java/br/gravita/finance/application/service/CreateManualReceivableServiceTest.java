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
	void createsAnOpenManualReceivableForARegisteredCustomer() {
		UUID customerId = UUID.randomUUID();
		LocalDate dueDate = LocalDate.now().plusDays(30);
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));
		when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Receivable created = service
				.execute(new CreateManualReceivableCommand(customerId, new BigDecimal("150.00"), dueDate, null));

		assertThat(created.getOrigin()).isEqualTo(ReceivableOrigin.MANUAL);
		assertThat(created.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(created.getCustomerId()).isEqualTo(customerId);
		assertThat(created.getAmount()).isEqualByComparingTo("150.00");
		assertThat(created.getDueDate()).isEqualTo(dueDate);
	}

	@Test
	void savesTheCreatedReceivable() {
		UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));
		when(receivableRepositoryPort.save(any(Receivable.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(
				new CreateManualReceivableCommand(customerId, BigDecimal.TEN, LocalDate.now().plusDays(1), 3));

		ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getInstallments()).isEqualTo(3);
	}

	@Test
	void rejectsAnUnregisteredCustomerWithoutSavingAnything() {
		UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new CreateManualReceivableCommand(customerId, BigDecimal.TEN, LocalDate.now().plusDays(1), null)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAZeroAmountWithoutSavingAnything() {
		UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder().build()));

		assertThatThrownBy(() -> service.execute(
				new CreateManualReceivableCommand(customerId, BigDecimal.ZERO, LocalDate.now().plusDays(1), null)))
				.isInstanceOf(BusinessRuleException.class);

		verify(receivableRepositoryPort, never()).save(any());
	}
}
