package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.BankStatementLine;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.inbound.finance.ReconcileBankStatementCommand;
import br.gravita.core.ports.inbound.finance.ReconcileBankStatementUseCase;
import br.gravita.core.ports.inbound.finance.ReconciliationResult;
import br.gravita.core.ports.outbound.finance.ImportBankStatementPort;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ReconcileBankStatementService implements ReconcileBankStatementUseCase {

	private final ImportBankStatementPort importBankStatementPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	public ReconcileBankStatementService(final ImportBankStatementPort importBankStatementPort,
			final SettlementRepositoryPort settlementRepositoryPort,
			final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort) {
		this.importBankStatementPort = importBankStatementPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.internalCashBoxRepositoryPort = internalCashBoxRepositoryPort;
	}

	/**
	 * Reads only: nothing is written, so importing the same statement again gives
	 * the same result. A line is paired with the oldest settlement (or, failing
	 * that, cash movement) that has exactly its value and date and was not
	 * already taken by an earlier line, so two identical lines need two
	 * counterparts. Dates are compared in UTC, the same day boundary the CNAB
	 * import stamps its settlements with.
	 */
	@Override
	@Transactional(readOnly = true)
	public ReconciliationResult execute(final ReconcileBankStatementCommand command) {
		final List<BankStatementLine> lines = importBankStatementPort.parse(command.fileContent());
		if (lines.isEmpty()) {
			return new ReconciliationResult(List.of(), List.of());
		}

		final Map<Key, Deque<UnaryOperator<BankStatementLine>>> candidates = candidatesFor(lines, command);

		final List<BankStatementLine> matched = new ArrayList<>();
		final List<BankStatementLine> unmatched = new ArrayList<>();
		for (final BankStatementLine line : lines) {
			final Deque<UnaryOperator<BankStatementLine>> available = candidates.get(Key.of(line.getPostedOn(),
					line.getAmount()));
			if (available == null || available.isEmpty()) {
				unmatched.add(line);
			} else {
				matched.add(available.poll().apply(line));
			}
		}
		return new ReconciliationResult(matched, unmatched);
	}

	/**
	 * What the statement could be paired with, by value and date: settlements first, then movements, each oldest first.
	 * Each entry turns a line into the same line matched to that counterpart.
	 */
	private Map<Key, Deque<UnaryOperator<BankStatementLine>>> candidatesFor(final List<BankStatementLine> lines,
			final ReconcileBankStatementCommand command) {
		final LocalDate first = lines.stream().map(BankStatementLine::getPostedOn).min(Comparator.naturalOrder())
				.orElseThrow();
		final LocalDate last = lines.stream().map(BankStatementLine::getPostedOn).max(Comparator.naturalOrder())
				.orElseThrow();
		final Instant from = first.atStartOfDay(ZoneOffset.UTC).toInstant();
		final Instant until = last.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

		final Map<Key, Deque<UnaryOperator<BankStatementLine>>> candidates = new HashMap<>();
		// Only the titles collected into / paid from this account can appear on its statement.
		for (final Settlement settlement : settlementRepositoryPort.findRealizedBetween(from, until,
				new CashFlowFilter(null, null, command.bankAccount(), null))) {
			final BigDecimal signed = settlement.isReceivableSide() ? settlement.cashAmount()
					: settlement.cashAmount().negate();
			candidates.computeIfAbsent(Key.of(dateOf(settlement.getTimestamp()), signed), k -> new ArrayDeque<>())
					.add(line -> line.matchedTo(settlement.getId()));
		}
		// Movements are not tied to a bank account, so they are offered to every statement.
		for (final CashMovement movement : internalCashBoxRepositoryPort.findMovementsBetween(from, until)) {
			// TO_BANK is a deposit, a credit on the statement; the box's own sign is the other way round.
			final BigDecimal signed = movement.getDirection() == CashMovementDirection.TO_BANK ? movement.getAmount()
					: movement.getAmount().negate();
			candidates.computeIfAbsent(Key.of(dateOf(movement.getTimestamp()), signed), k -> new ArrayDeque<>())
					.add(line -> line.matchedTo(movement.getId()));
		}
		return candidates;
	}

	private static LocalDate dateOf(final Instant instant) {
		return instant.atZone(ZoneOffset.UTC).toLocalDate();
	}

	/** Value and date; the amount is normalised so that 10.5 and 10.50 are the same value. */
	private record Key(LocalDate date, BigDecimal amount) {

		static Key of(final LocalDate date, final BigDecimal amount) {
			return new Key(date, amount.signum() == 0 ? BigDecimal.ZERO : amount.stripTrailingZeros());
		}
	}
}
