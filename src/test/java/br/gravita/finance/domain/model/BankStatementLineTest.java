package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.BankStatementLine;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BankStatementLineTest {

	private static final LocalDate DAY = LocalDate.of(2026, 9, 25);

	private BankStatementLine line(final String amount) {
		return BankStatementLine.unmatched(3, DAY, new BigDecimal(amount), "PIX RECEBIDO", "FIT-1");
	}

	@Test
	@DisplayName("Starts a statement line as unmatched")
	void startsUnmatched() {
		final BankStatementLine line = line("10.00");

		assertThat(line.isMatched()).isFalse();
		assertThat(line.getSettlementId()).isNull();
		assertThat(line.getCashMovementId()).isNull();
	}

	@Test
	@DisplayName("Returns a copy pointing at the settlement or the movement when matched")
	void matchingReturnsACopyPointingAtTheSettlementOrTheMovement() {
		final BankStatementLine line = line("10.00");
		final SettlementId settlementId = SettlementId.of(UUID.randomUUID());
		final CashMovementId movementId = CashMovementId.of(UUID.randomUUID());

		final BankStatementLine toSettlement = line.matchedTo(settlementId);
		final BankStatementLine toMovement = line.matchedTo(movementId);

		assertThat(toSettlement.isMatched()).isTrue();
		assertThat(toSettlement.getSettlementId()).isEqualTo(settlementId);
		assertThat(toSettlement.getCashMovementId()).isNull();
		assertThat(toMovement.getCashMovementId()).isEqualTo(movementId);
		assertThat(toMovement.getSettlementId()).isNull();
		assertThat(toSettlement.getLineNumber()).isEqualTo(3);
		assertThat(toSettlement.getPostedOn()).isEqualTo(DAY);
		assertThat(toSettlement.getAmount()).isEqualByComparingTo("10.00");
		assertThat(toSettlement.getDescription()).isEqualTo("PIX RECEBIDO");
		assertThat(toSettlement.getReference()).isEqualTo("FIT-1");
		assertThat(line.isMatched()).isFalse();
	}

	@Test
	@DisplayName("Tells credit from debit by the sign of the amount")
	void theSignOfTheAmountTellsCreditFromDebit() {
		assertThat(line("10.00").isCredit()).isTrue();
		assertThat(line("-10.00").isCredit()).isFalse();
	}

	@Test
	@DisplayName("Turns a missing description into an empty one")
	void missingDescriptionBecomesEmpty() {
		assertThat(BankStatementLine.unmatched(1, DAY, BigDecimal.ONE, null, null).getDescription()).isEmpty();
	}

	@Test
	@DisplayName("Rejects a line number below one")
	void rejectsALineNumberBelowOne() {
		assertThatThrownBy(() -> BankStatementLine.unmatched(0, DAY, BigDecimal.ONE, null, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires both a date and an amount")
	void requiresADateAndAnAmount() {
		assertThatThrownBy(() -> BankStatementLine.unmatched(1, null, BigDecimal.ONE, null, null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> BankStatementLine.unmatched(1, DAY, null, null, null))
				.isInstanceOf(NullPointerException.class);
	}
}
