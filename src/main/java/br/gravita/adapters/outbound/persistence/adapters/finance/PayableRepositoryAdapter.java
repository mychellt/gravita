package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.PayableJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.PayablePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.PayableJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class PayableRepositoryAdapter implements PayableRepositoryPort {

	private final PayableJpaRepository jpaRepository;
	private final PayablePersistenceMapper mapper;

	PayableRepositoryAdapter(PayableJpaRepository jpaRepository, PayablePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Payable save(Payable payable) {
		PayableJpaEntity entity = mapper.toEntity(payable);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PayableJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Payable> findById(PayableId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public List<Payable> findByPurchaseReceiptRef(UUID purchaseReceiptRef) {
		return jpaRepository.findByPurchaseReceiptRefOrderByInstallmentNumber(purchaseReceiptRef).stream()
				.map(mapper::toDomain).toList();
	}
}
