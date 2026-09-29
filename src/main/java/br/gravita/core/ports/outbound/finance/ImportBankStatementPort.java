package br.gravita.core.ports.outbound.finance;

import br.gravita.core.domain.finance.BankStatementLine;
import java.util.List;

/** Reads a bank statement file (OFX or CSV) into its entries. */
public interface ImportBankStatementPort {

	/**
	 * @return the statement's entries in file order, none of them matched yet
	 * @throws br.gravita.core.domain.shared.BusinessRuleException
	 *             if the payload isn't a valid OFX/CSV statement
	 */
	List<BankStatementLine> parse(String fileContent);
}
