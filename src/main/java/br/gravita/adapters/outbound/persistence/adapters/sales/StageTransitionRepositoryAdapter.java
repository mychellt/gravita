package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.mappers.sales.StageTransitionPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.StageTransitionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
import java.time.Instant;
import java.util.List;

@PersistenceAdapter
class StageTransitionRepositoryAdapter implements StageTransitionRepositoryPort {

	private final StageTransitionJpaRepository jpaRepository;
	private final StageTransitionPersistenceMapper mapper;

	StageTransitionRepositoryAdapter(StageTransitionJpaRepository jpaRepository,
			StageTransitionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public StageTransition save(StageTransition stageTransition) {
		return mapper.map(jpaRepository.save(mapper.map(stageTransition)));
	}

	@Override
	public List<StageTransition> findByOpportunityId(OpportunityId opportunityId) {
		return jpaRepository.findByOpportunityId(opportunityId.value()).stream().map(mapper::map).toList();
	}

	@Override
	public List<StageTransition> findByPeriod(Instant periodStart, Instant periodEnd) {
		return jpaRepository.findByTimestampGreaterThanEqualAndTimestampLessThan(periodStart, periodEnd).stream()
				.map(mapper::map)
				.toList();
	}
}
