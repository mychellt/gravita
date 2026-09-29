package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.AgingEntry;
import br.gravita.core.domain.finance.AgingReport;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.inbound.finance.GetAgingListQuery;
import br.gravita.core.ports.inbound.finance.GetAgingListUseCase;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class GetAgingListService implements GetAgingListUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;
	private final Clock clock;

	@Autowired
	public GetAgingListService(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort) {
		this(receivableRepositoryPort, settlementRepositoryPort, Clock.systemDefaultZone());
	}

	public GetAgingListService(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort, Clock clock) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
		this.clock = clock;
	}

	/**
	 * Only titles still to be received ({@code OPEN}/{@code PARTIALLY_SETTLED})
	 * that fall due on or before the query date count, each for what is left
	 * after its settlements. A cost center yields an empty report: receivables
	 * are not charged to one.
	 */
	@Override
	@Transactional(readOnly = true)
	public AgingReport execute(GetAgingListQuery query) {
		LocalDate asOfDate = query.asOfDate() != null ? query.asOfDate() : LocalDate.now(clock);
		if (query.costCenterId() != null) {
			return AgingReport.of(asOfDate, List.of());
		}
		List<Receivable> receivables = receivableRepositoryPort.findOutstandingByCustomerDueUntil(query.customerId(),
				asOfDate);
		if (receivables.isEmpty()) {
			return AgingReport.of(asOfDate, List.of());
		}
		Map<ReceivableId, List<Settlement>> settlements = new HashMap<>();
		settlementRepositoryPort.findByReceivableIds(receivables.stream().map(Receivable::getId).toList())
				.forEach(settlement -> settlements.computeIfAbsent(settlement.getReceivableId(), id -> new ArrayList<>())
						.add(settlement));
		List<AgingEntry> entries = receivables.stream()
				.filter(Receivable::isOutstanding)
				.map(receivable -> new AgingEntry(receivable.getDueDate(),
						receivable.remainingBalance(settlements.getOrDefault(receivable.getId(), List.of()))))
				.toList();
		return AgingReport.of(asOfDate, entries);
	}
}
