package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * The managerial DRE of a month, a read model projected from {@code sales}, {@code tax}, {@code inventory} and
 * {@code finance}. {@code netResult} reconciles as {@code grossRevenue - deductions - cmv - totalExpenses}, where
 * {@code totalExpenses} is the sum of {@code expensesByCostCenter}. {@code costCenter} echoes the filter the DRE was
 * asked for, {@code null} when it covers every cost center; revenue, deductions and CMV are not charged to cost
 * centers, so the filter only ever narrows the expenses.
 */
public record ManagerialDre(YearMonth period, UUID costCenter, BigDecimal grossRevenue, BigDecimal deductions,
		BigDecimal cmv, List<CostCenterExpense> expensesByCostCenter, BigDecimal totalExpenses,
		BigDecimal netResult) {

	/** What one cost center spent; a {@code null} {@code costCenterId} gathers the expenses charged to none. */
	public record CostCenterExpense(UUID costCenterId, BigDecimal amount) {
	}
}
