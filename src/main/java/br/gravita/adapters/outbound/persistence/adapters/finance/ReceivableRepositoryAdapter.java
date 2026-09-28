package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.ReceivablePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.ReceivableJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;

@PersistenceAdapter
class ReceivableRepositoryAdapter implements ReceivableRepositoryPort {

	private final ReceivableJpaRepository jpaRepository;
	private final ReceivablePersistenceMapper mapper;

	ReceivableRepositoryAdapter(ReceivableJpaRepository jpaRepository, ReceivablePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Receivable save(Receivable receivable) {
		ReceivableJpaEntity entity = mapper.toEntity(receivable);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		ReceivableJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
