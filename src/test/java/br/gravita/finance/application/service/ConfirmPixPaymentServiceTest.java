package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.ConfirmPixPaymentCommand;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.usercases.finance.ConfirmPixPaymentService;
import br.gravita.finance.PixPayloads;
import java.math.BigDecimal;
import java.time.Instant;
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
class ConfirmPixPaymentServiceTest {

	@Mock
	private PixChargeRepositoryPort pixChargeRepositoryPort;

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@InjectMocks
	private ConfirmPixPaymentService service;

	private Receivable receivable(ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal("150.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	private PixCharge charge(Receivable receivable, PixChargeStatus status) {
		return PixCharge.of(PixChargeId.of(UUID.randomUUID()), receivable.getId(), PixPayloads.valid(),
				receivable.getAmount(), receivable.getDueDate(), Instant.now().plusSeconds(3600), status);
	}

	@Test
	void marksTheChargePaidAndSettlesTheLinkedReceivable() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		PixCharge charge = charge(receivable, PixChargeStatus.PENDING);
		when(pixChargeRepositoryPort.findById(charge.getId())).thenReturn(Optional.of(charge));
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(pixChargeRepositoryPort.save(any(PixCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PixCharge paid = service.execute(new ConfirmPixPaymentCommand(charge.getId().value()));

		assertThat(paid.getStatus()).isEqualTo(PixChargeStatus.PAID);
		ArgumentCaptor<Receivable> settled = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(settled.capture());
		assertThat(settled.getValue().getId()).isEqualTo(receivable.getId());
		assertThat(settled.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void anExpiredChargeThatTheBankReportsPaidIsStillPaid() {
		Receivable receivable = receivable(ReceivableStatus.OPEN);
		PixCharge charge = charge(receivable, PixChargeStatus.EXPIRED);
		when(pixChargeRepositoryPort.findById(charge.getId())).thenReturn(Optional.of(charge));
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(pixChargeRepositoryPort.save(any(PixCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.execute(new ConfirmPixPaymentCommand(charge.getId().value())).getStatus())
				.isEqualTo(PixChargeStatus.PAID);
		verify(receivableRepositoryPort).save(any(Receivable.class));
	}

	@Test
	void aRepeatedConfirmationChangesNothing() {
		Receivable receivable = receivable(ReceivableStatus.SETTLED);
		PixCharge charge = charge(receivable, PixChargeStatus.PAID);
		when(pixChargeRepositoryPort.findById(charge.getId())).thenReturn(Optional.of(charge));

		PixCharge result = service.execute(new ConfirmPixPaymentCommand(charge.getId().value()));

		assertThat(result).isSameAs(charge);
		verify(pixChargeRepositoryPort, never()).save(any());
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void leavesAnAlreadySettledReceivableAsIsButMarksTheChargePaid() {
		Receivable receivable = receivable(ReceivableStatus.SETTLED);
		PixCharge charge = charge(receivable, PixChargeStatus.PENDING);
		when(pixChargeRepositoryPort.findById(charge.getId())).thenReturn(Optional.of(charge));
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(pixChargeRepositoryPort.save(any(PixCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertThat(service.execute(new ConfirmPixPaymentCommand(charge.getId().value())).getStatus())
				.isEqualTo(PixChargeStatus.PAID);
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	void failsWithoutMarkingPaidWhenTheReceivableCannotBeSettled() {
		Receivable receivable = receivable(ReceivableStatus.CANCELLED);
		PixCharge charge = charge(receivable, PixChargeStatus.PENDING);
		when(pixChargeRepositoryPort.findById(charge.getId())).thenReturn(Optional.of(charge));
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));

		assertThatThrownBy(() -> service.execute(new ConfirmPixPaymentCommand(charge.getId().value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(pixChargeRepositoryPort, never()).save(any());
	}

	@Test
	void failsWhenTheChargeDoesNotExist() {
		UUID missing = UUID.randomUUID();
		when(pixChargeRepositoryPort.findById(PixChargeId.of(missing))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ConfirmPixPaymentCommand(missing)))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
