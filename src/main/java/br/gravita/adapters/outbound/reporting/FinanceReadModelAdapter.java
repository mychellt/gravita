package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
class FinanceReadModelAdapter implements FinanceReadModelPort {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;

	FinanceReadModelAdapter(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public List<OverdueBalance> overdueReceivables(LocalDate asOf, UUID companyId) {
		List<Receivable> receivables = receivableRepositoryPort.findOutstandingDueUntil(asOf.minusDays(1),
				new CashFlowFilter(companyId, null, null, null));
		if (receivables.isEmpty()) {
			return List.of();
		}
		Map<ReceivableId, List<Settlement>> settlements = new HashMap<>();
		settlementRepositoryPort.findByReceivableIds(receivables.stream().map(Receivable::getId).toList())
				.forEach(settlement -> settlements.computeIfAbsent(settlement.getReceivableId(), id -> new ArrayList<>())
						.add(settlement));
		return receivables.stream().filter(Receivable::isOutstanding)
				.map(receivable -> new OverdueBalance(receivable.getDueDate(),
						receivable.remainingBalance(settlements.getOrDefault(receivable.getId(), List.of()))))
				.toList();
	}
}
