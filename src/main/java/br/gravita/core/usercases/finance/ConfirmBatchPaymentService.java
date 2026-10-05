package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult.RejectedLine;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult.UnmatchedLine;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentCommand;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BankReturnLine;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@UseCase
public class ConfirmBatchPaymentService implements ConfirmBatchPaymentUseCase {

	private final BankIntegrationPort bankIntegrationPort;
	private final PayableRepositoryPort payableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;

	/**
	 * The whole file is one transaction: a line that can't be confirmed for a
	 * business reason (unknown title, payable not approved, payment already
	 * imported, amount that doesn't clear the payable) is reported and skipped
	 * before anything is written for it, while a failure to persist rolls the
	 * import back so it can simply be run again.
	 */
	@Override
	@Transactional
	public BankReturnImportResult execute(final ConfirmBatchPaymentCommand command) {
		final List<BankReturnLine> lines = bankIntegrationPort.parseReturnFile(command.bankIntegration(),
				command.fileContent());

		int settled = 0;
		int skipped = 0;
		final List<UnmatchedLine> unmatched = new ArrayList<>();
		final List<RejectedLine> rejected = new ArrayList<>();
		for (final BankReturnLine line : lines) {
			if (line.rejected()) {
				// The payable is left as it is: still APPROVED, so it can be sent to the bank again.
				rejected.add(new RejectedLine(line.lineNumber(), line.titleIdentifier(), line.rejectionReason()));
				continue;
			}
			if (!line.paid()) {
				skipped++;
				continue;
			}
			final Optional<String> failure = confirm(line);
			if (failure.isPresent()) {
				unmatched.add(new UnmatchedLine(line.lineNumber(), line.titleIdentifier(), line.amount(),
						failure.get()));
			} else {
				settled++;
			}
		}
		return new BankReturnImportResult(settled, skipped, unmatched, rejected);
	}

	/** @return why the line could not be confirmed, or empty once it has been */
	private Optional<String> confirm(final BankReturnLine line) {
		final Optional<PayableId> payableId = parseTitleIdentifier(line.titleIdentifier());
		if (payableId.isEmpty()) {
			return Optional.of("Title identifier is not a payable id");
		}
		final Optional<Payable> found = payableRepositoryPort.findById(payableId.get());
		if (found.isEmpty()) {
			return Optional.of("No payable found for the title identifier");
		}
		final Payable payable = found.get();

		final Settlement settlement;
		final Payable paid;
		try {
			settlement = Settlement.automaticCnabForPayable(SettlementId.of(UUID.randomUUID()), payable.getId(),
					line.amount(), line.interest(), line.fine(), line.discount(), line.surcharge(),
					line.paidAt().atStartOfDay(ZoneOffset.UTC).toInstant());
			if (settlementRepositoryPort.findByPayableId(payable.getId()).stream()
					.anyMatch(settlement::isSamePaymentAs)) {
				return Optional.of("Payment already imported");
			}
			if (payable.getStatus() != PayableStatus.APPROVED) {
				return Optional.of("Payable is " + payable.getStatus() + ", not approved");
			}
			// A payable has no partially-paid state, so a payment that doesn't clear it stays for a person to look at.
			if (settlement.creditedAmount().compareTo(payable.getAmount()) != 0) {
				return Optional.of("Amount paid " + settlement.creditedAmount() + " does not match the payable amount "
						+ payable.getAmount());
			}
			paid = payable.pay(null);
		} catch (final BusinessRuleException e) {
			return Optional.of(e.getMessage());
		}

		settlementRepositoryPort.save(settlement);
		payableRepositoryPort.save(paid);
		return Optional.empty();
	}

	private static Optional<PayableId> parseTitleIdentifier(final String titleIdentifier) {
		try {
			return Optional.of(PayableId.of(UUID.fromString(titleIdentifier.trim())));
		} catch (final IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
