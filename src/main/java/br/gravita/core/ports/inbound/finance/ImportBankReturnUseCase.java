package br.gravita.core.ports.inbound.finance;

/**
 * UC-M8-05: settles the receivables paid according to a bank's CNAB 240/400
 * return file. Each paid line is matched to a receivable by its title
 * identifier and becomes an {@code AUTOMATIC_CNAB} settlement; a line that
 * can't be settled is reported in the result. Idempotent - importing the same
 * file again settles nothing twice.
 */
public interface ImportBankReturnUseCase {

	BankReturnImportResult execute(ImportBankReturnCommand command);
}
