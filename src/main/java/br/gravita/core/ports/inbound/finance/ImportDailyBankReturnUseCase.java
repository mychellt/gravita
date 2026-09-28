package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.util.Optional;

/**
 * Daily entry point of UC-M8-05, driven by a scheduler (the import has no REST
 * endpoint): fetches the bank's return file and imports it through
 * {@link ImportBankReturnUseCase}.
 */
public interface ImportDailyBankReturnUseCase {

	/** @return the import result, or empty if the bank has no return file to import */
	Optional<BankReturnImportResult> execute(BankIntegration bankIntegration);
}
