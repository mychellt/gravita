package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.RenegotiationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.RenegotiationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.RenegotiationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class RenegotiationRepositoryAdapter implements RenegotiationRepositoryPort {

	private final RenegotiationJpaRepository jpaRepository;
	private final RenegotiationPersistenceMapper mapper;

	RenegotiationRepositoryAdapter(RenegotiationJpaRepository jpaRepository, RenegotiationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Renegotiation save(Renegotiation renegotiation) {
		RenegotiationJpaEntity entity = mapper.toEntity(renegotiation);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<Renegotiation> findByOriginalReceivableId(ReceivableId receivableId) {
		return jpaRepository.findByOriginalReceivableId(receivableId.value()).map(mapper::toDomain);
	}
}
