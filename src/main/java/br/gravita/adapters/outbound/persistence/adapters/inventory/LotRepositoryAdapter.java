package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.LotJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.LotPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.LotJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class LotRepositoryAdapter implements LotRepositoryPort {

	private final LotJpaRepository jpaRepository;
	private final LotPersistenceMapper mapper;

	LotRepositoryAdapter(final LotJpaRepository jpaRepository, final LotPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<Lot> findByProductIdAndWarehouseIdAndCode(final UUID productId, final UUID warehouseId, final String code) {
		return jpaRepository.findByProductIdAndWarehouseIdAndCode(productId, warehouseId, code).map(mapper::map);
	}

	@Override
	public List<Lot> findByExpiryDateLessThanEqual(final LocalDate cutoffDate) {
		return jpaRepository.findByExpiryDateLessThanEqual(cutoffDate).stream().map(mapper::map).toList();
	}

	@Override
	public List<Lot> findByExpiryDateLessThanEqualAndWarehouseId(final LocalDate cutoffDate, final UUID warehouseId) {
		return jpaRepository.findByExpiryDateLessThanEqualAndWarehouseId(cutoffDate, warehouseId).stream()
				.map(mapper::map)
				.toList();
	}

	@Override
	public Lot save(final Lot lot) {
		final LotJpaEntity entity = mapper.map(lot);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.save(entity));
	}
}
