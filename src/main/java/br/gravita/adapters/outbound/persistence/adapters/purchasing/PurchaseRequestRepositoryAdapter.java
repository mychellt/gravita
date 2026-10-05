package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseRequestPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseRequestJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class PurchaseRequestRepositoryAdapter implements PurchaseRequestRepositoryPort {

	private final PurchaseRequestJpaRepository jpaRepository;
	private final PurchaseRequestPersistenceMapper mapper;

	PurchaseRequestRepositoryAdapter(final PurchaseRequestJpaRepository jpaRepository, final PurchaseRequestPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PurchaseRequest save(final PurchaseRequest purchaseRequest) {
		final PurchaseRequestJpaEntity entity = mapper.map(purchaseRequest);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final PurchaseRequestJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PurchaseRequest> findById(final PurchaseRequestId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public boolean existsOpenByOriginAndProductId(final PurchaseRequestOrigin origin, final UUID productId) {
		return jpaRepository.existsByOriginAndStatusAndItemsProductId(origin, PurchaseRequestStatus.OPEN, productId);
	}
}
