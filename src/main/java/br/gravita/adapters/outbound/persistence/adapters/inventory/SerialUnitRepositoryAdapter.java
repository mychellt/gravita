package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.SerialUnitJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.SerialUnitPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.SerialUnitJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import java.util.List;
import java.util.UUID;

@PersistenceAdapter
class SerialUnitRepositoryAdapter implements SerialUnitRepositoryPort {

	private final SerialUnitJpaRepository jpaRepository;
	private final SerialUnitPersistenceMapper mapper;

	SerialUnitRepositoryAdapter(final SerialUnitJpaRepository jpaRepository, final SerialUnitPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public List<SerialUnit> saveAll(final List<SerialUnit> serialUnits) {
		final List<SerialUnitJpaEntity> entities = serialUnits.stream().map(mapper::map).toList();
		entities.forEach(entity -> entity.setNew(!jpaRepository.existsById(entity.getId())));
		return jpaRepository.saveAll(entities).stream()
				.map(mapper::map)
				.toList();
	}

	@Override
	public List<SerialUnit> findByProductIdAndWarehouseIdAndSerialNumberIn(final UUID productId, final UUID warehouseId,
			final List<String> serialNumbers) {
		return jpaRepository.findByProductIdAndWarehouseIdAndSerialNumberIn(productId, warehouseId, serialNumbers)
				.stream()
				.map(mapper::map)
				.toList();
	}
}
