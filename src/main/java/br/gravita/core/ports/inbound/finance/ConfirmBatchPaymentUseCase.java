package br.gravita.core.ports.inbound.finance;

/**
 * UC-M8-22: confirms the payables paid according to a bank's CNAB 240/400
 * return for a payment remittance (UC-M8-13) or PIX transfer. Each confirmed
 * line is matched to an {@code APPROVED} payable by its title identifier and
 * becomes an {@code AUTOMATIC_CNAB} settlement that moves the payable to
 * {@code PAID}. A line the bank rejected leaves its payable {@code APPROVED}
 * and is reported in {@link BankReturnImportResult#rejectedLines()}; a line
 * that can't be matched is reported in {@link BankReturnImportResult#unmatchedLines()}.
 * Idempotent - importing the same file again settles nothing twice.
 */
public interface ConfirmBatchPaymentUseCase {

	BankReturnImportResult execute(ConfirmBatchPaymentCommand command);
}
