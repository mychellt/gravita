package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of importing one return file. {@code settledCount} lines produced a
 * baixa; {@code skippedCount} lines were not payments (registrations, ...) and
 * need no baixa; every paid line that could not be settled is in
 * {@code unmatchedLines} with the reason, so nothing is dropped silently.
 * {@code rejectedLines} are the lines the bank refused: they settle nothing and
 * are only reported by the payment side (UC-M8-22); a collection return
 * (UC-M8-05) counts them as skipped.
 */
public record BankReturnImportResult(int settledCount, int skippedCount, List<UnmatchedLine> unmatchedLines,
		List<RejectedLine> rejectedLines) {

	public BankReturnImportResult {
		unmatchedLines = List.copyOf(unmatchedLines);
		rejectedLines = List.copyOf(rejectedLines);
	}

	public BankReturnImportResult(final int settledCount, final int skippedCount, final List<UnmatchedLine> unmatchedLines) {
		this(settledCount, skippedCount, unmatchedLines, List.of());
	}

	public record UnmatchedLine(int lineNumber, String titleIdentifier, BigDecimal amount, String reason) {
	}

	/** A line the bank refused; {@code reason} is the bank's. */
	public record RejectedLine(int lineNumber, String titleIdentifier, String reason) {
	}
}
