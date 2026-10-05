package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.RenegotiationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.RenegotiationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.RenegotiationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class RenegotiationRepositoryAdapter implements RenegotiationRepositoryPort {

	private final RenegotiationJpaRepository jpaRepository;
	private final RenegotiationPersistenceMapper mapper;

	RenegotiationRepositoryAdapter(final RenegotiationJpaRepository jpaRepository, final RenegotiationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Renegotiation save(final Renegotiation renegotiation) {
		final RenegotiationJpaEntity entity = mapper.map(renegotiation);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public Optional<Renegotiation> findByOriginalReceivableId(final ReceivableId receivableId) {
		return jpaRepository.findByOriginalReceivableId(receivableId.value()).map(mapper::map);
	}

	@Override
	public List<Renegotiation> findByCustomerId(final UUID customerId) {
		return jpaRepository.findByCustomerIdOrderByRenegotiatedAtAsc(customerId).stream().map(mapper::map)
				.toList();
	}
}
