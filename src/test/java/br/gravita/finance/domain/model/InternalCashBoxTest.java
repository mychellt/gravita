package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InternalCashBoxTest {

	private static CashMovement movement(final InternalCashBoxId boxId, final CashMovementDirection direction, final String amount,
			final String justification) {
		return CashMovement.of(CashMovementId.of(UUID.randomUUID()), boxId, direction, new BigDecimal(amount),
				justification, Instant.now());
	}

	@Test
	@DisplayName("Records a movement with its direction, amount and justification")
	void movementRecordsItsDirectionAmountAndJustification() {
		final CashMovement movement = movement(InternalCashBoxId.MAIN, CashMovementDirection.TO_BANK, "150.00",
				"Deposit of the day's cash");

		assertThat(movement.getDirection()).isEqualTo(CashMovementDirection.TO_BANK);
		assertThat(movement.getAmount()).isEqualByComparingTo("150.00");
		assertThat(movement.getJustification()).isEqualTo("Deposit of the day's cash");
		assertThat(movement.getCashBoxId()).isEqualTo(InternalCashBoxId.MAIN);
	}

	@Test
	@DisplayName("Requires a movement to have a positive amount")
	void movementRequiresAPositiveAmount() {
		assertThatThrownBy(() -> movement(InternalCashBoxId.MAIN, CashMovementDirection.TO_BANK, "0", "x"))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> movement(InternalCashBoxId.MAIN, CashMovementDirection.FROM_BANK, "-1.00", "x"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires a movement to have a non-blank justification")
	void movementRequiresANonBlankJustification() {
		assertThatThrownBy(() -> movement(InternalCashBoxId.MAIN, CashMovementDirection.TO_BANK, "10", " "))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("justification");
		assertThatThrownBy(() -> movement(InternalCashBoxId.MAIN, CashMovementDirection.TO_BANK, "10", null))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("justification");
	}

	@Test
	@DisplayName("Adds to the balance on a movement from the bank and subtracts on a movement to the bank")
	void fromBankAddsToTheBalanceAndToBankSubtractsFromIt() {
		final InternalCashBox box = InternalCashBox.of(InternalCashBoxId.MAIN, new BigDecimal("100.00"));

		final InternalCashBox afterWithdrawal = box
				.apply(movement(InternalCashBoxId.MAIN, CashMovementDirection.FROM_BANK, "50.00", "Change float"));
		final InternalCashBox afterDeposit = afterWithdrawal
				.apply(movement(InternalCashBoxId.MAIN, CashMovementDirection.TO_BANK, "120.00", "Deposit"));

		assertThat(afterWithdrawal.getBalance()).isEqualByComparingTo("150.00");
		assertThat(afterDeposit.getBalance()).isEqualByComparingTo("30.00");
		assertThat(box.getBalance()).isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("Rejects a movement that belongs to another cash box")
	void rejectsAMovementOfAnotherCashBox() {
		final InternalCashBox box = InternalCashBox.of(InternalCashBoxId.MAIN, BigDecimal.ZERO);
		final CashMovement other = movement(InternalCashBoxId.of(UUID.randomUUID()), CashMovementDirection.FROM_BANK,
				"10.00", "x");

		assertThatThrownBy(() -> box.apply(other)).isInstanceOf(BusinessRuleException.class);
	}
}
