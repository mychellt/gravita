package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseReceiptPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseReceiptJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class PurchaseReceiptRepositoryAdapter implements PurchaseReceiptRepositoryPort {

	private final PurchaseReceiptJpaRepository jpaRepository;
	private final PurchaseReceiptPersistenceMapper mapper;

	PurchaseReceiptRepositoryAdapter(PurchaseReceiptJpaRepository jpaRepository,
			PurchaseReceiptPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PurchaseReceipt save(PurchaseReceipt purchaseReceipt) {
		PurchaseReceiptJpaEntity entity = mapper.map(purchaseReceipt);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PurchaseReceiptJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PurchaseReceipt> findById(PurchaseReceiptId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<PurchaseReceipt> findByOrderId(PurchaseOrderId orderId) {
		return jpaRepository.findByOrderId(orderId.value()).stream().map(mapper::map).toList();
	}
}
