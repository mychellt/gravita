package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.CnabRemittance;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.BatchPayCommand;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedRemittance;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.RemittanceRequest;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.BatchPayService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchPayServiceTest {

	private static final IssuedRemittance ISSUED = new IssuedRemittance("REM-0001", "cnab-payload");

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private BankIntegrationPort bankIntegrationPort;

	@InjectMocks
	private BatchPayService service;

	private Payable payable(PayableStatus status, String amount) {
		return Payable.of(PayableId.of(UUID.randomUUID()), UUID.randomUUID(), PayableOrigin.MANUAL,
				new BigDecimal(amount), LocalDate.now().plusDays(5), null, status, null, null, null);
	}

	private BatchPayCommand command(Payable... payables) {
		return new BatchPayCommand(List.of(payables).stream().map(payable -> payable.getId().value()).toList(),
				BankIntegration.ITAU);
	}

	private void found(Payable... payables) {
		when(payableRepositoryPort.findByIds(any())).thenReturn(List.of(payables));
	}

	@Test
	@DisplayName("Generates a single remittance covering every selected payable")
	void generatesASingleRemittanceCoveringEverySelectedPayable() {
		Payable first = payable(PayableStatus.APPROVED, "100.00");
		Payable second = payable(PayableStatus.APPROVED, "250.50");
		found(second, first);
		when(bankIntegrationPort.sendRemittance(any())).thenReturn(ISSUED);

		CnabRemittance remittance = service.execute(command(first, second));

		assertThat(remittance.reference()).isEqualTo("REM-0001");
		assertThat(remittance.fileContent()).isEqualTo("cnab-payload");
		assertThat(remittance.bankIntegration()).isEqualTo(BankIntegration.ITAU);
		assertThat(remittance.payableIds()).containsExactly(first.getId(), second.getId());
		assertThat(remittance.totalAmount()).isEqualByComparingTo("350.50");

		ArgumentCaptor<RemittanceRequest> request = ArgumentCaptor.forClass(RemittanceRequest.class);
		verify(bankIntegrationPort).sendRemittance(request.capture());
		assertThat(request.getValue().bankIntegration()).isEqualTo(BankIntegration.ITAU);
		assertThat(request.getValue().items()).hasSize(2);
		assertThat(request.getValue().items().get(0).payableId()).isEqualTo(first.getId().value());
		assertThat(request.getValue().items().get(0).supplierId()).isEqualTo(first.getSupplierId());
		assertThat(request.getValue().items().get(0).amount()).isEqualByComparingTo("100.00");
		assertThat(request.getValue().items().get(1).payableId()).isEqualTo(second.getId().value());
	}

	@Test
	@DisplayName("Neither saves nor changes the payables when generating the remittance")
	void doesNotSaveOrChangeThePayables() {
		Payable approved = payable(PayableStatus.APPROVED, "100.00");
		found(approved);
		when(bankIntegrationPort.sendRemittance(any())).thenReturn(ISSUED);

		service.execute(command(approved));

		assertThat(approved.getStatus()).isEqualTo(PayableStatus.APPROVED);
		verify(payableRepositoryPort).findByIds(any());
		verifyNoMoreInteractions(payableRepositoryPort);
	}

	@Test
	@DisplayName("Rejects a batch containing a payable that is not approved and names it in the error")
	void rejectsABatchContainingAPayableThatIsNotApprovedAndNamesIt() {
		Payable approved = payable(PayableStatus.APPROVED, "100.00");
		Payable open = payable(PayableStatus.OPEN, "50.00");
		Payable paid = payable(PayableStatus.PAID, "70.00");
		found(approved, open, paid);

		assertThatThrownBy(() -> service.execute(command(approved, open, paid)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining(open.getId().value() + " is OPEN")
				.hasMessageContaining(paid.getId().value() + " is PAID")
				.hasMessageNotContaining(approved.getId().value().toString());

		verifyNoInteractions(bankIntegrationPort);
	}

	@Test
	@DisplayName("Rejects a cancelled payable in the batch")
	void rejectsACancelledPayable() {
		Payable cancelled = payable(PayableStatus.CANCELLED, "100.00");
		found(cancelled);

		assertThatThrownBy(() -> service.execute(command(cancelled))).isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(bankIntegrationPort);
	}

	@Test
	@DisplayName("Rejects an unknown payable in the batch")
	void rejectsAnUnknownPayable() {
		Payable approved = payable(PayableStatus.APPROVED, "100.00");
		UUID unknown = UUID.randomUUID();
		found(approved);

		assertThatThrownBy(() -> service.execute(
				new BatchPayCommand(List.of(approved.getId().value(), unknown), BankIntegration.ITAU)))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining(unknown.toString());

		verifyNoInteractions(bankIntegrationPort);
	}

	@Test
	@DisplayName("Rejects a batch with no payables selected")
	void rejectsAnEmptySelection() {
		assertThatThrownBy(() -> service.execute(new BatchPayCommand(List.of(), BankIntegration.ITAU)))
				.isInstanceOf(BusinessRuleException.class);

		verifyNoInteractions(payableRepositoryPort, bankIntegrationPort);
	}

	@Test
	@DisplayName("Rejects a batch that selects the same payable twice")
	void rejectsAPayableSelectedTwice() {
		UUID id = UUID.randomUUID();

		assertThatThrownBy(() -> service.execute(new BatchPayCommand(List.of(id, id), BankIntegration.ITAU)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining(id.toString());

		verifyNoInteractions(payableRepositoryPort, bankIntegrationPort);
	}

	@Test
	@DisplayName("Propagates the failure when the bank cannot accept the remittance")
	void propagatesABankThatCannotTakeTheRemittance() {
		Payable approved = payable(PayableStatus.APPROVED, "100.00");
		found(approved);
		when(bankIntegrationPort.sendRemittance(any()))
				.thenThrow(new BankIntegrationUnavailableException("Bank integration not configured: ITAU"));

		assertThatThrownBy(() -> service.execute(command(approved)))
				.isInstanceOf(BankIntegrationUnavailableException.class);
	}
}
