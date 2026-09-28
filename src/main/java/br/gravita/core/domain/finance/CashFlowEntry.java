package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One movement feeding a {@link CashFlowProjection}: money that came in or
 * went out ({@code realized}, a settlement) or is expected to ({@code projected},
 * an open title) on {@code date}.
 */
public record CashFlowEntry(LocalDate date, Direction direction, boolean realized, BigDecimal amount) {

	public enum Direction {
		INFLOW,
		OUTFLOW
	}

	public CashFlowEntry {
		Objects.requireNonNull(date, "date is required");
		Objects.requireNonNull(direction, "direction is required");
		Objects.requireNonNull(amount, "amount is required");
	}

	public static CashFlowEntry realizedInflow(LocalDate date, BigDecimal amount) {
		return new CashFlowEntry(date, Direction.INFLOW, true, amount);
	}

	public static CashFlowEntry realizedOutflow(LocalDate date, BigDecimal amount) {
		return new CashFlowEntry(date, Direction.OUTFLOW, true, amount);
	}

	public static CashFlowEntry projectedInflow(LocalDate date, BigDecimal amount) {
		return new CashFlowEntry(date, Direction.INFLOW, false, amount);
	}

	public static CashFlowEntry projectedOutflow(LocalDate date, BigDecimal amount) {
		return new CashFlowEntry(date, Direction.OUTFLOW, false, amount);
	}
}
