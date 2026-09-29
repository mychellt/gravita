package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.BankStatementLine;
import java.util.List;

/**
 * Outcome of reconciling one statement, both lists in statement order. Every
 * line of the statement is in exactly one of them: {@code matched} lines point
 * at the settlement or cash movement they were paired with, {@code unmatched}
 * lines are left for a manual review.
 */
public record ReconciliationResult(List<BankStatementLine> matched, List<BankStatementLine> unmatched) {

	public ReconciliationResult {
		matched = List.copyOf(matched);
		unmatched = List.copyOf(unmatched);
	}
}
