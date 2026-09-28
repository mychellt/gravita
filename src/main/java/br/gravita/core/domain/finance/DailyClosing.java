package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * End-of-day summary of one {@link InternalCashBox} (§9.3): what came in from the
 * bank ({@code FROM_BANK}), what went out to it ({@code TO_BANK}) and the balance
 * before and after. Totals are derived from the day's {@link CashMovement}s, so
 * they always match them. Not the PDV's Z-report (M3). Immutable.
 */
@Getter
public final class DailyClosing {

	private final InternalCashBoxId cashBoxId;
	private final LocalDate date;
	private final BigDecimal openingBalance;
	private final BigDecimal entries;
	private final BigDecimal exits;
	private final BigDecimal closingBalance;
	private final List<CashMovement> movements;

	private DailyClosing(InternalCashBoxId cashBoxId, LocalDate date, BigDecimal openingBalance,
			List<CashMovement> movements) {
		this.cashBoxId = Objects.requireNonNull(cashBoxId, "cashBoxId is required");
		this.date = Objects.requireNonNull(date, "date is required");
		this.openingBalance = Objects.requireNonNull(openingBalance, "openingBalance is required");
		this.movements = List.copyOf(Objects.requireNonNull(movements, "movements is required"));
		this.entries = sum(this.movements, CashMovementDirection.FROM_BANK);
		this.exits = sum(this.movements, CashMovementDirection.TO_BANK);
		this.closingBalance = openingBalance.add(entries).subtract(exits);
	}

	/**
	 * @param openingBalance the box balance when {@code date} started, i.e. the closing balance of the day before
	 * @param movements every movement of {@code cashBoxId} that happened on {@code date}
	 */
	public static DailyClosing of(InternalCashBoxId cashBoxId, LocalDate date, BigDecimal openingBalance,
			List<CashMovement> movements) {
		return new DailyClosing(cashBoxId, date, openingBalance, movements);
	}

	private static BigDecimal sum(List<CashMovement> movements, CashMovementDirection direction) {
		return movements.stream()
				.filter(movement -> movement.getDirection() == direction)
				.map(CashMovement::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
