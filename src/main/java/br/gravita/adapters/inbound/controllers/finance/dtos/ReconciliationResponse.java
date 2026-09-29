package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.BankStatementLine;
import br.gravita.core.ports.inbound.finance.ReconciliationResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * {@code unmatched} are the statement lines that need a manual review. A matched
 * line names the {@code settlementId} or the {@code cashMovementId} it was paired with.
 */
public record ReconciliationResponse(int matchedCount, int unmatchedCount, List<StatementLine> matched,
		List<StatementLine> unmatched) {

	public static ReconciliationResponse from(ReconciliationResult result) {
		return new ReconciliationResponse(result.matched().size(), result.unmatched().size(),
				result.matched().stream().map(StatementLine::from).toList(),
				result.unmatched().stream().map(StatementLine::from).toList());
	}

	public record StatementLine(int lineNumber, LocalDate date, BigDecimal amount, String description,
			String reference, UUID settlementId, UUID cashMovementId) {

		static StatementLine from(BankStatementLine line) {
			return new StatementLine(line.getLineNumber(), line.getPostedOn(), line.getAmount(),
					line.getDescription(), line.getReference(),
					line.getSettlementId() == null ? null : line.getSettlementId().value(),
					line.getCashMovementId() == null ? null : line.getCashMovementId().value());
		}
	}
}
