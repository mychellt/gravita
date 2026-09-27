package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.StageTransition;
import java.util.List;

public interface StageTransitionRepositoryPort {
	StageTransition save(StageTransition stageTransition);

	/**
	 * Query shape UC-M7-15 (Get Funnel Conversion, Phase 4) will use to compute
	 * average cycle time between stages from this append-only log.
	 */
	List<StageTransition> findByOpportunityId(OpportunityId opportunityId);
}
