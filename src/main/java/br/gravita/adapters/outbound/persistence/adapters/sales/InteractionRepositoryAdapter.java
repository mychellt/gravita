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

	InteractionRepositoryAdapter(final InteractionJpaRepository jpaRepository, final InteractionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Interaction save(final Interaction interaction) {
		return mapper.map(jpaRepository.save(mapper.map(interaction)));
	}

	@Override
	public List<Interaction> findByOpportunityId(final OpportunityId opportunityId) {
		return jpaRepository.findByOpportunityId(opportunityId.value()).stream().map(mapper::map).toList();
	}

	@Override
	public List<Interaction> findByCustomerId(final UUID customerId) {
		return jpaRepository.findByCustomerId(customerId).stream().map(mapper::map).toList();
	}
}
