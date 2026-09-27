package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.mappers.sales.StageTransitionPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.StageTransitionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
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
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(stageTransition)));
	}

	@Override
	public List<StageTransition> findByOpportunityId(OpportunityId opportunityId) {
		return jpaRepository.findByOpportunityId(opportunityId.value()).stream().map(mapper::toDomain).toList();
	}
}
