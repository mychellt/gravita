package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockTransferJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockTransferPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockTransferJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;
import br.gravita.core.ports.outbound.persistence.inventory.StockTransferRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class StockTransferRepositoryAdapter implements StockTransferRepositoryPort {

	private final StockTransferJpaRepository jpaRepository;
	private final StockTransferPersistenceMapper mapper;

	StockTransferRepositoryAdapter(StockTransferJpaRepository jpaRepository, StockTransferPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public StockTransfer save(StockTransfer transfer) {
		StockTransferJpaEntity entity = mapper.toEntity(transfer);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<StockTransfer> findById(StockTransferId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
