package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.DailyClosing;
import br.gravita.core.domain.finance.InternalCashBoxId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DailyClosingTest {

	private static final LocalDate DAY = LocalDate.of(2026, 9, 28);

	private static CashMovement movement(CashMovementDirection direction, String amount) {
		return CashMovement.of(CashMovementId.of(UUID.randomUUID()), InternalCashBoxId.MAIN, direction,
				new BigDecimal(amount), "Reason", Instant.parse("2026-09-28T12:00:00Z"));
	}

	@Test
	@DisplayName("Computes totals equal to the sum of the day's movements")
	void totalsMatchTheSumOfTheDaysMovements() {
		DailyClosing closing = DailyClosing.of(InternalCashBoxId.MAIN, DAY, new BigDecimal("100.00"), List.of(
				movement(CashMovementDirection.FROM_BANK, "50.00"),
				movement(CashMovementDirection.FROM_BANK, "25.50"),
				movement(CashMovementDirection.TO_BANK, "30.25")));

		assertThat(closing.getEntries()).isEqualByComparingTo("75.50");
		assertThat(closing.getExits()).isEqualByComparingTo("30.25");
		assertThat(closing.getOpeningBalance()).isEqualByComparingTo("100.00");
		assertThat(closing.getClosingBalance()).isEqualByComparingTo("145.25");
		assertThat(closing.getMovements()).hasSize(3);
	}

	@Test
	@DisplayName("Closes a day without movements at the opening balance")
	void aDayWithoutMovementsClosesAtTheOpeningBalance() {
		DailyClosing closing = DailyClosing.of(InternalCashBoxId.MAIN, DAY, new BigDecimal("80.00"), List.of());

		assertThat(closing.getEntries()).isEqualByComparingTo("0");
		assertThat(closing.getExits()).isEqualByComparingTo("0");
		assertThat(closing.getClosingBalance()).isEqualByComparingTo("80.00");
	}
}
