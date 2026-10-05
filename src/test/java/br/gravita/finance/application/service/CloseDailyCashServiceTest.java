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
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.DailyClosing;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CloseDailyCashCommand;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import br.gravita.core.usercases.finance.CloseDailyCashService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseDailyCashServiceTest {

	private static final LocalDate DAY = LocalDate.of(2026, 9, 28);
	private static final Instant DAY_START = Instant.parse("2026-09-28T00:00:00Z");

	@Mock
	private InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	private CloseDailyCashService service;

	@BeforeEach
	void setUp() {
		final Clock clock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);
		service = new CloseDailyCashService(internalCashBoxRepositoryPort, clock);
	}

	private static CashMovement movement(final CashMovementDirection direction, final String amount, final String at) {
		return CashMovement.of(CashMovementId.of(UUID.randomUUID()), InternalCashBoxId.MAIN, direction,
				new BigDecimal(amount), "Reason", Instant.parse(at));
	}

	private void boxWithBalance(final String balance, final CashMovement... fromDayStart) {
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN))
				.thenReturn(Optional.of(InternalCashBox.of(InternalCashBoxId.MAIN, new BigDecimal(balance))));
		when(internalCashBoxRepositoryPort.findMovementsFrom(InternalCashBoxId.MAIN, DAY_START))
				.thenReturn(List.of(fromDayStart));
	}

	@Test
	@DisplayName("Summarises the entries, exits and balances of the day")
	void summarisesEntriesExitsAndBalancesOfTheDay() {
		// opening 100 + 60 - 25 = 135 at the end of the day
		boxWithBalance("135.00",
				movement(CashMovementDirection.FROM_BANK, "60.00", "2026-09-28T09:00:00Z"),
				movement(CashMovementDirection.TO_BANK, "25.00", "2026-09-28T17:30:00Z"));

		final DailyClosing closing = service.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, DAY));

		assertThat(closing.getEntries()).isEqualByComparingTo("60.00");
		assertThat(closing.getExits()).isEqualByComparingTo("25.00");
		assertThat(closing.getOpeningBalance()).isEqualByComparingTo("100.00");
		assertThat(closing.getClosingBalance()).isEqualByComparingTo("135.00");
		assertThat(closing.getMovements()).hasSize(2);
	}

	@Test
	@DisplayName("Ignores movements of later days in the closing even though they change the current balance")
	void movementsOfLaterDaysMoveTheCurrentBalanceButNotThisClosing() {
		// end of 28th: 100 + 40 = 140; on the 29th 30 more went to the bank, so the box holds 110 now
		boxWithBalance("110.00",
				movement(CashMovementDirection.FROM_BANK, "40.00", "2026-09-28T23:59:59Z"),
				movement(CashMovementDirection.TO_BANK, "30.00", "2026-09-29T00:00:00Z"));

		final DailyClosing closing = service.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, DAY));

		assertThat(closing.getOpeningBalance()).isEqualByComparingTo("100.00");
		assertThat(closing.getEntries()).isEqualByComparingTo("40.00");
		assertThat(closing.getExits()).isEqualByComparingTo("0");
		assertThat(closing.getClosingBalance()).isEqualByComparingTo("140.00");
		assertThat(closing.getMovements()).hasSize(1);
	}

	@Test
	@DisplayName("Uses the prior day's closing balance as the opening balance")
	void openingBalanceEqualsThePriorDaysClosingBalance() {
		// Box moves: 27th: +50 (opening 20, closing 70); 28th: -10 (opening 70, closing 60)
		final CashMovement dayBefore = movement(CashMovementDirection.FROM_BANK, "50.00", "2026-09-27T10:00:00Z");
		final CashMovement day = movement(CashMovementDirection.TO_BANK, "10.00", "2026-09-28T10:00:00Z");
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN))
				.thenReturn(Optional.of(InternalCashBox.of(InternalCashBoxId.MAIN, new BigDecimal("60.00"))));
		when(internalCashBoxRepositoryPort.findMovementsFrom(InternalCashBoxId.MAIN, DAY_START))
				.thenReturn(List.of(day));
		when(internalCashBoxRepositoryPort.findMovementsFrom(InternalCashBoxId.MAIN,
				Instant.parse("2026-09-27T00:00:00Z"))).thenReturn(List.of(dayBefore, day));

		final DailyClosing previous = service
				.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, DAY.minusDays(1)));
		final DailyClosing closing = service.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, DAY));

		assertThat(previous.getClosingBalance()).isEqualByComparingTo("70.00");
		assertThat(closing.getOpeningBalance()).isEqualByComparingTo(previous.getClosingBalance());
	}

	@Test
	@DisplayName("Keeps the balance unchanged on a day without movements")
	void dayWithoutMovementsKeepsTheBalance() {
		boxWithBalance("80.00");

		final DailyClosing closing = service.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, DAY));

		assertThat(closing.getOpeningBalance()).isEqualByComparingTo("80.00");
		assertThat(closing.getClosingBalance()).isEqualByComparingTo("80.00");
		assertThat(closing.getMovements()).isEmpty();
	}

	@Test
	@DisplayName("Rejects closing the cash for a date in the future")
	void rejectsADateInTheFuture() {
		assertThatThrownBy(() -> service.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN,
				LocalDate.of(2026, 10, 1)))).isInstanceOf(BusinessRuleException.class);

		verify(internalCashBoxRepositoryPort, never()).findByIdForUpdate(any());
	}

	@Test
	@DisplayName("Fails when the cash box does not exist")
	void failsWhenTheCashBoxDoesNotExist() {
		when(internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new CloseDailyCashCommand(InternalCashBoxId.MAIN, DAY)))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
