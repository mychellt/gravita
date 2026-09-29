package br.gravita.core.ports.inbound.finance;

/**
 * UC-M8-18: imports an OFX/CSV bank statement and pairs each line with the
 * settlement (of a receivable or of a payable) or internal cash movement that
 * has the same value and date. Distinct from UC-M8-05, which settles receivables
 * from the bank's CNAB return: this one only reads what was already recorded and
 * changes nothing. A settlement or movement is paired with at most one line; a
 * line without a counterpart is reported as unmatched for manual review.
 */
public interface ReconcileBankStatementUseCase {

	ReconciliationResult execute(ReconcileBankStatementCommand command);
}
