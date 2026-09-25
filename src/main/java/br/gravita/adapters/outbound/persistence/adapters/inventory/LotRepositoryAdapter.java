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

	LotRepositoryAdapter(LotJpaRepository jpaRepository, LotPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<Lot> findByProductIdAndWarehouseIdAndCode(UUID productId, UUID warehouseId, String code) {
		return jpaRepository.findByProductIdAndWarehouseIdAndCode(productId, warehouseId, code).map(mapper::toDomain);
	}

	@Override
	public List<Lot> findByExpiryDateLessThanEqual(LocalDate cutoffDate) {
		return jpaRepository.findByExpiryDateLessThanEqual(cutoffDate).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Lot> findByExpiryDateLessThanEqualAndWarehouseId(LocalDate cutoffDate, UUID warehouseId) {
		return jpaRepository.findByExpiryDateLessThanEqualAndWarehouseId(cutoffDate, warehouseId).stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public Lot save(Lot lot) {
		LotJpaEntity entity = mapper.toEntity(lot);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}
}
