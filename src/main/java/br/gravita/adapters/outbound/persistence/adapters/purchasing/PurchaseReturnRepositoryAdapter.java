package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReturnJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseReturnPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.PurchaseReturnJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReturnRepositoryPort;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class PurchaseReturnRepositoryAdapter implements PurchaseReturnRepositoryPort {

	private final PurchaseReturnJpaRepository jpaRepository;
	private final PurchaseReturnPersistenceMapper mapper;

	PurchaseReturnRepositoryAdapter(PurchaseReturnJpaRepository jpaRepository, PurchaseReturnPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PurchaseReturn save(PurchaseReturn purchaseReturn) {
		PurchaseReturnJpaEntity entity = mapper.map(purchaseReturn);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PurchaseReturnJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PurchaseReturn> findById(PurchaseReturnId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<PurchaseReturn> findByReceiptId(PurchaseReceiptId receiptId) {
		return jpaRepository.findByReceiptId(receiptId.value()).stream().map(mapper::map).toList();
	}
}
