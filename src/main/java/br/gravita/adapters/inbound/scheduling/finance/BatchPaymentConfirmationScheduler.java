package br.gravita.adapters.inbound.scheduling.finance;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ConfirmDailyBatchPaymentUseCase;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives {@link ConfirmDailyBatchPaymentUseCase} (UC-M8-22) once a day for every bank listed in
 * {@code gravita.finance.payment-return.banks} (none by default) - the confirmation has no REST endpoint, so this
 * is its only trigger. A bank that fails doesn't stop the others; the next run picks the file up again.
 */
@Slf4j
@Component
public class BatchPaymentConfirmationScheduler {

	private final ConfirmDailyBatchPaymentUseCase confirmDailyBatchPaymentUseCase;
	private final List<BankIntegration> banks;

	public BatchPaymentConfirmationScheduler(final ConfirmDailyBatchPaymentUseCase confirmDailyBatchPaymentUseCase,
			@Value("${gravita.finance.payment-return.banks:}") final List<BankIntegration> banks) {
		this.confirmDailyBatchPaymentUseCase = confirmDailyBatchPaymentUseCase;
		this.banks = banks;
	}

	@Scheduled(cron = "${gravita.finance.payment-return.confirm-cron:0 0 7 * * *}")
	public void confirmPayments() {
		for (final BankIntegration bank : banks) {
			try {
				confirmDailyBatchPaymentUseCase.execute(bank).ifPresentOrElse(result -> report(bank, result),
						() -> log.info("No payment return file to confirm for {}", bank));
			} catch (final RuntimeException e) {
				log.error("Payment return confirmation failed for {}", bank, e);
			}
		}
	}

	private void report(final BankIntegration bank, final BankReturnImportResult result) {
		log.info("Payment return confirmed for {}: {} settled, {} skipped, {} rejected, {} unmatched", bank,
				result.settledCount(), result.skippedCount(), result.rejectedLines().size(),
				result.unmatchedLines().size());
		result.rejectedLines().forEach(line -> log.warn("Bank rejected payment line {} for {} (payable {}): {}",
				line.lineNumber(), bank, line.titleIdentifier(), line.reason()));
		result.unmatchedLines().forEach(line -> log.warn("Unmatched payment return line {} for {} (title {}): {}",
				line.lineNumber(), bank, line.titleIdentifier(), line.reason()));
	}
}
