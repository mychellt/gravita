package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.ReceivablePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.ReceivableJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

	@Override
	public Optional<Receivable> findById(ReceivableId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public List<Receivable> findByOriginDocumentRef(UUID originDocumentRef) {
		return jpaRepository.findByOriginDocumentRefOrderByInstallmentNumber(originDocumentRef).stream()
				.map(mapper::toDomain).toList();
	}
}
