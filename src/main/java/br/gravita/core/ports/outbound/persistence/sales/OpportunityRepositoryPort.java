package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import java.util.List;
import java.util.Optional;

public interface OpportunityRepositoryPort {
	Opportunity save(Opportunity opportunity);

	Optional<Opportunity> findById(OpportunityId id);

	List<Opportunity> findAll();

	List<Opportunity> findByStage(OpportunityStage stage);
}
