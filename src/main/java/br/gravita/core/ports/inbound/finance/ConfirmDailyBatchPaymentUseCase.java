package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.util.Optional;

/**
 * Daily entry point of UC-M8-22, driven by a scheduler (the confirmation has no
 * REST endpoint): fetches the bank's payment return file and confirms it
 * through {@link ConfirmBatchPaymentUseCase}.
 */
public interface ConfirmDailyBatchPaymentUseCase {

	/** @return the import result, or empty if the bank has no payment return file to import */
	Optional<BankReturnImportResult> execute(BankIntegration bankIntegration);
}
