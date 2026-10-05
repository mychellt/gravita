package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult.UnmatchedLine;
import br.gravita.core.ports.inbound.finance.ImportBankReturnCommand;
import br.gravita.core.ports.inbound.finance.ImportBankReturnUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BankReturnLine;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ImportBankReturnService implements ImportBankReturnUseCase {

	private final BankIntegrationPort bankIntegrationPort;
	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;

	public ImportBankReturnService(final BankIntegrationPort bankIntegrationPort,
			final ReceivableRepositoryPort receivableRepositoryPort, final SettlementRepositoryPort settlementRepositoryPort) {
		this.bankIntegrationPort = bankIntegrationPort;
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
	}

	/**
	 * The whole file is one transaction: a line that can't be settled for a
	 * business reason (unknown title, title not open, payment already imported,
	 * invalid amounts) is reported and skipped before anything is written for
	 * it, while a failure to persist rolls the import back so it can simply be
	 * run again.
	 */
	@Override
	@Transactional
	public BankReturnImportResult execute(final ImportBankReturnCommand command) {
		final List<BankReturnLine> lines = bankIntegrationPort.parseReturnFile(command.bankIntegration(),
				command.fileContent());

		int settled = 0;
		int skipped = 0;
		final List<UnmatchedLine> unmatched = new ArrayList<>();
		for (final BankReturnLine line : lines) {
			if (!line.paid()) {
				skipped++;
				continue;
			}
			final Optional<String> rejection = settle(line);
			if (rejection.isPresent()) {
				unmatched.add(new UnmatchedLine(line.lineNumber(), line.titleIdentifier(), line.amount(),
						rejection.get()));
			} else {
				settled++;
			}
		}
		return new BankReturnImportResult(settled, skipped, unmatched);
	}

	/** @return why the line could not be settled, or empty once it has been */
	private Optional<String> settle(final BankReturnLine line) {
		final Optional<ReceivableId> receivableId = parseTitleIdentifier(line.titleIdentifier());
		if (receivableId.isEmpty()) {
			return Optional.of("Title identifier is not a receivable id");
		}
		final Optional<Receivable> found = receivableRepositoryPort.findById(receivableId.get());
		if (found.isEmpty()) {
			return Optional.of("No receivable found for the title identifier");
		}
		final Receivable receivable = found.get();

		final Settlement settlement;
		final Receivable updated;
		try {
			settlement = Settlement.automaticCnab(SettlementId.of(UUID.randomUUID()), receivable.getId(),
					line.amount(), line.interest(), line.fine(), line.discount(), line.surcharge(),
					line.paidAt().atStartOfDay(ZoneOffset.UTC).toInstant());
			final List<Settlement> previous = settlementRepositoryPort.findByReceivableId(receivable.getId());
			if (previous.stream().anyMatch(settlement::isSamePaymentAs)) {
				return Optional.of("Payment already imported");
			}
			if (receivable.getStatus() != ReceivableStatus.OPEN
					&& receivable.getStatus() != ReceivableStatus.PARTIALLY_SETTLED) {
				return Optional.of("Receivable is " + receivable.getStatus() + ", not open");
			}
			final BigDecimal totalCredited = previous.stream().map(Settlement::creditedAmount)
					.reduce(settlement.creditedAmount(), BigDecimal::add);
			updated = receivable.applyCreditedTotal(totalCredited);
		} catch (final BusinessRuleException e) {
			return Optional.of(e.getMessage());
		}

		settlementRepositoryPort.save(settlement);
		receivableRepositoryPort.save(updated);
		return Optional.empty();
	}

	private static Optional<ReceivableId> parseTitleIdentifier(final String titleIdentifier) {
		try {
			return Optional.of(ReceivableId.of(UUID.fromString(titleIdentifier.trim())));
		} catch (final IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
