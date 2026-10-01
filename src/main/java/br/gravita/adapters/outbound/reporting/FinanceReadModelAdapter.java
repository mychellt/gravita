package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import java.math.BigDecimal;
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
	private final PayableRepositoryPort payableRepositoryPort;

	FinanceReadModelAdapter(ReceivableRepositoryPort receivableRepositoryPort,
			SettlementRepositoryPort settlementRepositoryPort, PayableRepositoryPort payableRepositoryPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.payableRepositoryPort = payableRepositoryPort;
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

	/** A payable's expense falls in the month it is due: titles carry no competence date of their own. */
	@Override
	@Transactional(readOnly = true)
	public List<CostCenterExpense> expensesByCostCenter(LocalDate from, LocalDate to, UUID costCenterId,
			UUID companyId) {
		List<Payable> payables = payableRepositoryPort.findNotCancelledDueBetween(from, to,
				new CashFlowFilter(companyId, null, null, costCenterId));
		Map<UUID, BigDecimal> byCostCenter = new HashMap<>();
		for (Payable payable : payables) {
			if (payable.getOrigin() != PayableOrigin.MANUAL) {
				continue;
			}
			if (payable.getCostCenterSplit().isEmpty()) {
				if (costCenterId == null) {
					byCostCenter.merge(null, payable.getAmount(), BigDecimal::add);
				}
				continue;
			}
			for (CostCenterShare share : payable.getCostCenterSplit()) {
				if (costCenterId == null || costCenterId.equals(share.costCenterId())) {
					byCostCenter.merge(share.costCenterId(), payable.shareOf(payable.getAmount(), share.costCenterId()),
							BigDecimal::add);
				}
			}
		}
		return byCostCenter.entrySet().stream().map(entry -> new CostCenterExpense(entry.getKey(), entry.getValue()))
				.toList();
	}
}
