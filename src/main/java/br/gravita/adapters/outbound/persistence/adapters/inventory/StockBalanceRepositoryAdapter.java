package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockBalancePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class StockBalanceRepositoryAdapter implements StockBalanceRepositoryPort {

	private final StockBalanceJpaRepository jpaRepository;
	private final StockBalancePersistenceMapper mapper;

	StockBalanceRepositoryAdapter(StockBalanceJpaRepository jpaRepository, StockBalancePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<StockBalance> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId) {
		return jpaRepository.findByProductIdAndWarehouseId(productId, warehouseId).map(mapper::map);
	}

	@Override
	public List<StockBalance> findByProductId(UUID productId) {
		return jpaRepository.findByProductId(productId).stream().map(mapper::map).toList();
	}

	@Override
	public List<StockBalance> findByWarehouseId(UUID warehouseId) {
		return jpaRepository.findByWarehouseId(warehouseId).stream().map(mapper::map).toList();
	}

	@Override
	public List<StockBalance> findAll() {
		return jpaRepository.findAll().stream().map(mapper::map).toList();
	}

	@Override
	public StockBalance save(StockBalance balance) {
		StockBalanceJpaEntity entity = mapper.map(balance);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.save(entity));
	}
}
