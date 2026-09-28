package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import java.util.List;

public interface SettlementRepositoryPort {
	Settlement save(Settlement settlement);

	/** The baixas applied to a receivable, oldest first. */
	List<Settlement> findByReceivableId(ReceivableId receivableId);
}
