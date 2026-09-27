package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.OpportunityJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.OpportunityPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.OpportunityJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class OpportunityRepositoryAdapter implements OpportunityRepositoryPort {

	private final OpportunityJpaRepository jpaRepository;
	private final OpportunityPersistenceMapper mapper;

	OpportunityRepositoryAdapter(OpportunityJpaRepository jpaRepository, OpportunityPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Opportunity save(Opportunity opportunity) {
		OpportunityJpaEntity entity = mapper.toEntity(opportunity);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		OpportunityJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Opportunity> findById(OpportunityId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public List<Opportunity> findAll() {
		return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Opportunity> findByStage(OpportunityStage stage) {
		return jpaRepository.findByStage(stage).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Opportunity> findByIds(Collection<OpportunityId> ids) {
		List<UUID> uuids = ids.stream().map(OpportunityId::value).toList();
		return jpaRepository.findAllById(uuids).stream().map(mapper::toDomain).toList();
	}
}
