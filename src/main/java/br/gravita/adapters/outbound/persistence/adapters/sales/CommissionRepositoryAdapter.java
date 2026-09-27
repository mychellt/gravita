package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.CommissionPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.CommissionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.Commission;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRepositoryPort;

@PersistenceAdapter
class CommissionRepositoryAdapter implements CommissionRepositoryPort {

	private final CommissionJpaRepository jpaRepository;
	private final CommissionPersistenceMapper mapper;

	CommissionRepositoryAdapter(CommissionJpaRepository jpaRepository, CommissionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Commission save(Commission commission) {
		CommissionJpaEntity entity = mapper.toEntity(commission);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		CommissionJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
