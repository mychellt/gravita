package br.gravita.core.ports.outbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinanceReadModelPort {

	/**
	 * The receivables still to be received that fell due before {@code asOf}, each for what is left after its
	 * settlements. Restricted to {@code companyId}'s titles unless it is {@code null}.
	 */
	List<OverdueBalance> overdueReceivables(LocalDate asOf, UUID companyId);

	/**
	 * The expenses (manual payables, not cancelled) falling due over {@code [from, to]} (inclusive), summed per cost
	 * center in no particular order; a payable split across cost centers counts for each one's share, and one charged
	 * to none counts under a {@code null} cost center. Purchases of goods are not expenses: they reach the result as
	 * CMV once sold. A {@code costCenterId} keeps only that cost center's share; a {@code null} {@code companyId}
	 * does not restrict.
	 */
	List<CostCenterExpense> expensesByCostCenter(LocalDate from, LocalDate to, UUID costCenterId, UUID companyId);

	record OverdueBalance(LocalDate dueDate, BigDecimal outstanding) {
	}

	record CostCenterExpense(UUID costCenterId, BigDecimal amount) {
	}
}
