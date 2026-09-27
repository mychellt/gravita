package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.mappers.sales.InteractionPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.InteractionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.ports.outbound.persistence.sales.InteractionRepositoryPort;
import java.util.List;
import java.util.UUID;

@PersistenceAdapter
class InteractionRepositoryAdapter implements InteractionRepositoryPort {

	private final InteractionJpaRepository jpaRepository;
	private final InteractionPersistenceMapper mapper;

	InteractionRepositoryAdapter(InteractionJpaRepository jpaRepository, InteractionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Interaction save(Interaction interaction) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(interaction)));
	}

	@Override
	public List<Interaction> findByOpportunityId(OpportunityId opportunityId) {
		return jpaRepository.findByOpportunityId(opportunityId.value()).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Interaction> findByCustomerId(UUID customerId) {
		return jpaRepository.findByCustomerId(customerId).stream().map(mapper::toDomain).toList();
	}
}
