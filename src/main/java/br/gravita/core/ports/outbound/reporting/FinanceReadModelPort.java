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

	record OverdueBalance(LocalDate dueDate, BigDecimal outstanding) {
	}
}
