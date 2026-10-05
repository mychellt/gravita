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

	OpportunityRepositoryAdapter(final OpportunityJpaRepository jpaRepository, final OpportunityPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Opportunity save(final Opportunity opportunity) {
		final OpportunityJpaEntity entity = mapper.map(opportunity);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final OpportunityJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<Opportunity> findById(final OpportunityId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<Opportunity> findAll() {
		return jpaRepository.findAll().stream().map(mapper::map).toList();
	}

	@Override
	public List<Opportunity> findByStage(final OpportunityStage stage) {
		return jpaRepository.findByStage(stage).stream().map(mapper::map).toList();
	}

	@Override
	public List<Opportunity> findByIds(final Collection<OpportunityId> ids) {
		final List<UUID> uuids = ids.stream().map(OpportunityId::value).toList();
		return jpaRepository.findAllById(uuids).stream().map(mapper::map).toList();
	}
}
