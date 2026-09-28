package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.CashFlowBucket;
import br.gravita.core.domain.finance.CashFlowEntry;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CashFlowProjection;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GetCashFlowQuery;
import br.gravita.core.ports.inbound.finance.GetCashFlowUseCase;
import br.gravita.core.ports.outbound.finance.NegativeBalanceProjectionAlert;
import br.gravita.core.ports.outbound.finance.NotifyNegativeBalanceProjectionPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class GetCashFlowService implements GetCashFlowUseCase {

	private static final Logger log = LoggerFactory.getLogger(GetCashFlowService.class);

	static final int DEFAULT_DAYS_BACK = 30;
	static final int DEFAULT_DAYS_AHEAD = 90;
	static final long MAX_SPAN_DAYS = 3660;

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final PayableRepositoryPort payableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final NotifyNegativeBalanceProjectionPort notifyNegativeBalanceProjectionPort;
	private final Clock clock;

	@Autowired
	public GetCashFlowService(ReceivableRepositoryPort receivableRepositoryPort,
			PayableRepositoryPort payableRepositoryPort, SettlementRepositoryPort settlementRepositoryPort,
			NotifyNegativeBalanceProjectionPort notifyNegativeBalanceProjectionPort) {
		this(receivableRepositoryPort, payableRepositoryPort, settlementRepositoryPort,
				notifyNegativeBalanceProjectionPort, Clock.systemDefaultZone());
	}

	public GetCashFlowService(ReceivableRepositoryPort receivableRepositoryPort,
			PayableRepositoryPort payableRepositoryPort, SettlementRepositoryPort settlementRepositoryPort,
			NotifyNegativeBalanceProjectionPort notifyNegativeBalanceProjectionPort, Clock clock) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.payableRepositoryPort = payableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.notifyNegativeBalanceProjectionPort = notifyNegativeBalanceProjectionPort;
		this.clock = clock;
	}

	/**
	 * Realized: every settlement in the range counts on the day it was made, as
	 * the cash that moved (see {@link Settlement#cashAmount()}). Projected: an
	 * open receivable counts for what is still owed on it and an open payable
	 * for its amount, on the due date; a title already overdue counts today, as
	 * it is expected to be settled from now on. A cost center restricts the view
	 * to payables and counts only the share of each one charged to it.
	 */
	@Override
	@Transactional(readOnly = true)
	public CashFlowProjection execute(GetCashFlowQuery query) {
		LocalDate today = LocalDate.now(clock);
		LocalDate from = query.from() != null ? query.from() : today.minusDays(DEFAULT_DAYS_BACK);
		LocalDate to = query.to() != null ? query.to() : today.plusDays(DEFAULT_DAYS_AHEAD);
		if (to.isBefore(from)) {
			throw new BusinessRuleException("to must not be before from: " + from + " > " + to);
		}
		if (to.toEpochDay() - from.toEpochDay() > MAX_SPAN_DAYS) {
			throw new BusinessRuleException("The period must not exceed " + MAX_SPAN_DAYS + " days");
		}

		CashFlowFilter filter = query.filter();
		List<CashFlowEntry> entries = new ArrayList<>();
		addRealized(entries, from, to, filter);
		addOpenReceivables(entries, to, today, filter);
		addOpenPayables(entries, to, today, filter);

		CashFlowProjection projection = CashFlowProjection.of(query.granularity(), from, to, query.openingBalance(),
				entries);
		alertIfNegative(projection, filter, today);
		return projection;
	}

	private void addRealized(List<CashFlowEntry> entries, LocalDate from, LocalDate to, CashFlowFilter filter) {
		ZoneId zone = clock.getZone();
		Instant start = from.atStartOfDay(zone).toInstant();
		Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
		List<Settlement> settlements = settlementRepositoryPort.findRealizedBetween(start, end, filter);

		Map<PayableId, Payable> payables = filter.costCenterId() == null ? Map.of() : payablesOf(settlements);
		for (Settlement settlement : settlements) {
			LocalDate date = settlement.getTimestamp().atZone(zone).toLocalDate();
			BigDecimal cash = settlement.cashAmount();
			if (settlement.isReceivableSide()) {
				entries.add(CashFlowEntry.realizedInflow(date, cash));
				continue;
			}
			Payable payable = payables.get(settlement.getPayableId());
			BigDecimal share = filter.costCenterId() == null ? cash
					: payable == null ? BigDecimal.ZERO : payable.shareOf(cash, filter.costCenterId());
			if (share.signum() > 0) {
				entries.add(CashFlowEntry.realizedOutflow(date, share));
			}
		}
	}

	private Map<PayableId, Payable> payablesOf(List<Settlement> settlements) {
		List<PayableId> ids = settlements.stream().filter(settlement -> !settlement.isReceivableSide())
				.map(Settlement::getPayableId).distinct().toList();
		Map<PayableId, Payable> byId = new HashMap<>();
		if (!ids.isEmpty()) {
			payableRepositoryPort.findByIds(ids).forEach(payable -> byId.put(payable.getId(), payable));
		}
		return byId;
	}

	private void addOpenReceivables(List<CashFlowEntry> entries, LocalDate to, LocalDate today,
			CashFlowFilter filter) {
		List<Receivable> receivables = receivableRepositoryPort.findOutstandingDueUntil(to, filter);
		if (receivables.isEmpty()) {
			return;
		}
		Map<ReceivableId, List<Settlement>> settlements = new HashMap<>();
		settlementRepositoryPort.findByReceivableIds(receivables.stream().map(Receivable::getId).toList())
				.forEach(settlement -> settlements.computeIfAbsent(settlement.getReceivableId(), id -> new ArrayList<>())
						.add(settlement));
		for (Receivable receivable : receivables) {
			BigDecimal remaining = receivable.remainingBalance(settlements.getOrDefault(receivable.getId(), List.of()));
			if (remaining.signum() > 0) {
				entries.add(CashFlowEntry.projectedInflow(expectedOn(receivable.getDueDate(), today), remaining));
			}
		}
	}

	private void addOpenPayables(List<CashFlowEntry> entries, LocalDate to, LocalDate today, CashFlowFilter filter) {
		for (Payable payable : payableRepositoryPort.findOutstandingDueUntil(to, filter)) {
			BigDecimal amount = payable.shareOf(payable.getAmount(), filter.costCenterId());
			if (amount.signum() > 0) {
				entries.add(CashFlowEntry.projectedOutflow(expectedOn(payable.getDueDate(), today), amount));
			}
		}
	}

	private static LocalDate expectedOn(LocalDate dueDate, LocalDate today) {
		return dueDate.isBefore(today) ? today : dueDate;
	}

	/**
	 * A failing notification must not take the read down with it: the view is
	 * what the caller asked for, the alert is a side effect.
	 */
	private void alertIfNegative(CashFlowProjection projection, CashFlowFilter filter, LocalDate today) {
		Optional<CashFlowBucket> firstNegative = projection.firstNegativeBucket(today);
		if (firstNegative.isEmpty()) {
			return;
		}
		BigDecimal lowest = projection.lowestBalance(today).orElse(firstNegative.get().balance());
		try {
			notifyNegativeBalanceProjectionPort.notify(new NegativeBalanceProjectionAlert(filter,
					projection.getGranularity(), firstNegative.get().periodStart(), lowest));
		} catch (RuntimeException exception) {
			log.error("Could not send the negative balance projection alert for {}", filter, exception);
		}
	}
}
