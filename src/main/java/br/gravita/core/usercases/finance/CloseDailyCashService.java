package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.DailyClosing;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CloseDailyCashCommand;
import br.gravita.core.ports.inbound.finance.CloseDailyCashUseCase;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * The opening balance is the box balance when the day started: the current
 * balance minus every movement from that day on. That makes it equal the prior
 * day's closing balance by construction, whatever was recorded since.
 */
@UseCase
public class CloseDailyCashService implements CloseDailyCashUseCase {

	private final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;
	private final Clock clock;

	@Autowired
	public CloseDailyCashService(final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort) {
		this(internalCashBoxRepositoryPort, Clock.systemDefaultZone());
	}

	public CloseDailyCashService(final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort, final Clock clock) {
		this.internalCashBoxRepositoryPort = internalCashBoxRepositoryPort;
		this.clock = clock;
	}

	/** Locks the box so the balance and the movements read here are one consistent snapshot. */
	@Override
	@Transactional
	public DailyClosing execute(final CloseDailyCashCommand command) {
		final LocalDate date = command.date();
		if (date.isAfter(LocalDate.now(clock))) {
			throw new BusinessRuleException("Cannot close a day that has not started yet: " + date);
		}

		final InternalCashBox cashBox = internalCashBoxRepositoryPort.findByIdForUpdate(command.account())
				.orElseThrow(() -> new ResourceNotFoundException("Internal cash box not found"));

		final Instant dayStart = date.atStartOfDay(clock.getZone()).toInstant();
		final Instant nextDayStart = date.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
		final List<CashMovement> fromDayStart = internalCashBoxRepositoryPort.findMovementsFrom(cashBox.getId(), dayStart);

		final BigDecimal openingBalance = fromDayStart.stream()
				.map(CashMovement::signedAmount)
				.reduce(cashBox.getBalance(), BigDecimal::subtract);
		final List<CashMovement> ofTheDay = fromDayStart.stream()
				.filter(movement -> movement.getTimestamp().isBefore(nextDayStart))
				.toList();

		return DailyClosing.of(cashBox.getId(), date, openingBalance, ofTheDay);
	}
}
