package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseOrderPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseOrderJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class PurchaseOrderRepositoryAdapter implements PurchaseOrderRepositoryPort {

	private final PurchaseOrderJpaRepository jpaRepository;
	private final PurchaseOrderPersistenceMapper mapper;

	PurchaseOrderRepositoryAdapter(final PurchaseOrderJpaRepository jpaRepository, final PurchaseOrderPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PurchaseOrder save(final PurchaseOrder purchaseOrder) {
		final PurchaseOrderJpaEntity entity = mapper.map(purchaseOrder);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final PurchaseOrderJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PurchaseOrder> findById(final PurchaseOrderId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}
}
