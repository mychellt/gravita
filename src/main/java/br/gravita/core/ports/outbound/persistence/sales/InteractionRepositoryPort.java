package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.OpportunityId;
import java.util.List;
import java.util.UUID;

public interface InteractionRepositoryPort {
	Interaction save(Interaction interaction);

	/**
	 * Query shape UC-M7-12 (Evaluate Follow-up Rules) will use to find the most
	 * recent interaction logged against an opportunity.
	 */
	List<Interaction> findByOpportunityId(OpportunityId opportunityId);

	/**
	 * Query shape UC-M7-12 (Evaluate Follow-up Rules) will use to find the most
	 * recent interaction logged against a customer.
	 */
	List<Interaction> findByCustomerId(UUID customerId);
}
