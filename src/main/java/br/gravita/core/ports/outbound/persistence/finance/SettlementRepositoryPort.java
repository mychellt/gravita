package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface SettlementRepositoryPort {
	Settlement save(Settlement settlement);

	/** The baixas applied to a receivable, oldest first. */
	List<Settlement> findByReceivableId(ReceivableId receivableId);

	/** The baixas applied to a payable, oldest first. */
	List<Settlement> findByPayableId(PayableId payableId);

	/** The baixas applied to any of the given receivables, oldest first. */
	List<Settlement> findByReceivableIds(Collection<ReceivableId> receivableIds);

	/**
	 * The baixas, of receivables and of payables, made from {@code from}
	 * (inclusive) to {@code until} (exclusive) whose title matches {@code filter};
	 * a cost center only matches baixas of payables that charge it.
	 */
	List<Settlement> findRealizedBetween(Instant from, Instant until, CashFlowFilter filter);
}
