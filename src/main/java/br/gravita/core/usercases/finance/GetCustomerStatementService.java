package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.CustomerStatement;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GetCustomerStatementQuery;
import br.gravita.core.ports.inbound.finance.GetCustomerStatementUseCase;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class GetCustomerStatementService implements GetCustomerStatementUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final RenegotiationRepositoryPort renegotiationRepositoryPort;
	private final Clock clock;

	@Autowired
	public GetCustomerStatementService(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort,
			RenegotiationRepositoryPort renegotiationRepositoryPort) {
		this(receivableRepositoryPort, settlementRepositoryPort, renegotiationRepositoryPort,
				Clock.systemDefaultZone());
	}

	public GetCustomerStatementService(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort,
			RenegotiationRepositoryPort renegotiationRepositoryPort, Clock clock) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.renegotiationRepositoryPort = renegotiationRepositoryPort;
		this.clock = clock;
	}

	/**
	 * The open balance is what is still owed on every outstanding title (its
	 * amount less what its settlements credited), whatever the period: it is
	 * the customer's position now, not at the end of the period.
	 */
	@Override
	@Transactional(readOnly = true)
	public CustomerStatement execute(GetCustomerStatementQuery query) {
		LocalDate from = query.from();
		LocalDate to = query.to();
		if (from != null && to != null && to.isBefore(from)) {
			throw new BusinessRuleException("to must not be before from: " + from + " > " + to);
		}

		List<Receivable> receivables = receivableRepositoryPort.findByCustomerId(query.customerId());
		List<Settlement> settlements = settlementRepositoryPort
				.findByReceivableIds(receivables.stream().map(Receivable::getId).toList());
		BigDecimal openBalance = openBalance(receivables, settlements);

		List<Receivable> titles = receivables.stream().filter(receivable -> within(receivable.getDueDate(), from, to))
				.sorted(Comparator.comparing(Receivable::getDueDate)
						.thenComparing(Receivable::getInstallmentNumber, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();
		List<Settlement> statementSettlements = settlements.stream()
				.filter(settlement -> within(settlement.getTimestamp(), from, to))
				.sorted(Comparator.comparing(Settlement::getTimestamp)).toList();
		List<Renegotiation> renegotiations = renegotiationRepositoryPort.findByCustomerId(query.customerId())
				.stream().filter(renegotiation -> within(renegotiation.getCreatedAt(), from, to))
				.sorted(Comparator.comparing(Renegotiation::getCreatedAt)).toList();

		return new CustomerStatement(query.customerId(), from, to, titles, statementSettlements, renegotiations,
				openBalance);
	}

	private static BigDecimal openBalance(List<Receivable> receivables, List<Settlement> settlements) {
		return receivables.stream().filter(Receivable::isOutstanding)
				.map(receivable -> receivable.remainingBalance(settlements.stream()
						.filter(settlement -> settlement.getReceivableId().equals(receivable.getId())).toList()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static boolean within(LocalDate date, LocalDate from, LocalDate to) {
		return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
	}

	private boolean within(Instant instant, LocalDate from, LocalDate to) {
		return within(instant.atZone(clock.getZone()).toLocalDate(), from, to);
	}
}
