package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of importing one return file. {@code settledCount} lines produced a
 * baixa; {@code skippedCount} lines were not payments (registrations,
 * rejections, ...) and need no baixa; every paid line that could not be settled
 * is in {@code unmatchedLines} with the reason, so nothing is dropped silently.
 */
public record BankReturnImportResult(int settledCount, int skippedCount, List<UnmatchedLine> unmatchedLines) {

	public BankReturnImportResult {
		unmatchedLines = List.copyOf(unmatchedLines);
	}

	public record UnmatchedLine(int lineNumber, String titleIdentifier, BigDecimal amount, String reason) {
	}
}
