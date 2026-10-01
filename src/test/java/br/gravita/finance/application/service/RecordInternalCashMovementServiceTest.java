package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementCommand;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import br.gravita.core.usercases.finance.RecordInternalCashMovementService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordInternalCashMovementServiceTest {

	@Mock
	private InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	@InjectMocks
	private RecordInternalCashMovementService service;

	private void boxWithBalance(String balance) {
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN))
				.thenReturn(Optional.of(InternalCashBox.of(InternalCashBoxId.MAIN, new BigDecimal(balance))));
		when(internalCashBoxRepositoryPort.save(any(InternalCashBox.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(internalCashBoxRepositoryPort.saveMovement(any(CashMovement.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("Records the movement with its direction, amount and justification")
	void recordsTheMovementWithDirectionAmountAndJustification() {
		boxWithBalance("500.00");

		CashMovement recorded = service.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.TO_BANK, new BigDecimal("200.00"), "Deposit of the day's cash"));

		assertThat(recorded.getDirection()).isEqualTo(CashMovementDirection.TO_BANK);
		assertThat(recorded.getAmount()).isEqualByComparingTo("200.00");
		assertThat(recorded.getJustification()).isEqualTo("Deposit of the day's cash");
		assertThat(recorded.getCashBoxId()).isEqualTo(InternalCashBoxId.MAIN);
		assertThat(recorded.getTimestamp()).isNotNull();
		verify(internalCashBoxRepositoryPort).saveMovement(recorded);
	}

	@Test
	@DisplayName("Decreases the cash box balance on a movement to the bank")
	void toBankDecreasesTheCashBoxBalance() {
		boxWithBalance("500.00");

		service.execute(new RecordInternalCashMovementCommand(CashMovementDirection.TO_BANK,
				new BigDecimal("200.00"), "Deposit"));

		ArgumentCaptor<InternalCashBox> saved = ArgumentCaptor.forClass(InternalCashBox.class);
		verify(internalCashBoxRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getBalance()).isEqualByComparingTo("300.00");
	}

	@Test
	@DisplayName("Increases the cash box balance on a movement from the bank")
	void fromBankIncreasesTheCashBoxBalance() {
		boxWithBalance("500.00");

		service.execute(new RecordInternalCashMovementCommand(CashMovementDirection.FROM_BANK,
				new BigDecimal("75.50"), "Change float"));

		ArgumentCaptor<InternalCashBox> saved = ArgumentCaptor.forClass(InternalCashBox.class);
		verify(internalCashBoxRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getBalance()).isEqualByComparingTo("575.50");
	}

	@Test
	@DisplayName("Rejects a blank justification without changing the balance")
	void rejectsABlankJustificationWithoutChangingTheBalance() {
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN))
				.thenReturn(Optional.of(InternalCashBox.of(InternalCashBoxId.MAIN, BigDecimal.TEN)));

		assertThatThrownBy(() -> service.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.TO_BANK, new BigDecimal("5.00"), "  ")))
				.isInstanceOf(BusinessRuleException.class);

		verify(internalCashBoxRepositoryPort, never()).save(any(InternalCashBox.class));
		verify(internalCashBoxRepositoryPort, never()).saveMovement(any(CashMovement.class));
	}

	@Test
	@DisplayName("Rejects an amount that is zero or negative")
	void rejectsANonPositiveAmount() {
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN))
				.thenReturn(Optional.of(InternalCashBox.of(InternalCashBoxId.MAIN, BigDecimal.TEN)));

		assertThatThrownBy(() -> service.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.FROM_BANK, BigDecimal.ZERO, "Nothing")))
				.isInstanceOf(BusinessRuleException.class);

		verify(internalCashBoxRepositoryPort, never()).saveMovement(any(CashMovement.class));
	}

	@Test
	@DisplayName("Fails when the cash box does not exist")
	void failsWhenTheCashBoxDoesNotExist() {
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new RecordInternalCashMovementCommand(
				CashMovementDirection.TO_BANK, BigDecimal.TEN, "Deposit")))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
