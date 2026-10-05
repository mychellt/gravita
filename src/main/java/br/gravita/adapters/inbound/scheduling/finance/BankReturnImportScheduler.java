package br.gravita.adapters.inbound.scheduling.finance;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ImportDailyBankReturnUseCase;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives {@link ImportDailyBankReturnUseCase} (UC-M8-05) once a day for every bank listed in
 * {@code gravita.finance.bank-return.banks} (none by default) - the import has no REST endpoint, so this
 * is its only trigger. A bank that fails doesn't stop the others; the next run picks the file up again.
 */
@Slf4j
@Component
public class BankReturnImportScheduler {

	private final ImportDailyBankReturnUseCase importDailyBankReturnUseCase;
	private final List<BankIntegration> banks;

	public BankReturnImportScheduler(final ImportDailyBankReturnUseCase importDailyBankReturnUseCase,
			@Value("${gravita.finance.bank-return.banks:}") final List<BankIntegration> banks) {
		this.importDailyBankReturnUseCase = importDailyBankReturnUseCase;
		this.banks = banks;
	}

	@Scheduled(cron = "${gravita.finance.bank-return.import-cron:0 0 6 * * *}")
	public void importReturns() {
		for (final BankIntegration bank : banks) {
			try {
				importDailyBankReturnUseCase.execute(bank).ifPresentOrElse(result -> report(bank, result),
						() -> log.info("No bank return file to import for {}", bank));
			} catch (final RuntimeException e) {
				log.error("Bank return import failed for {}", bank, e);
			}
		}
	}

	private void report(final BankIntegration bank, final BankReturnImportResult result) {
		log.info("Bank return imported for {}: {} settled, {} skipped, {} unmatched", bank, result.settledCount(),
				result.skippedCount(), result.unmatchedLines().size());
		result.unmatchedLines().forEach(line -> log.warn("Unmatched bank return line {} for {} (title {}): {}",
				line.lineNumber(), bank, line.titleIdentifier(), line.reason()));
	}
}
