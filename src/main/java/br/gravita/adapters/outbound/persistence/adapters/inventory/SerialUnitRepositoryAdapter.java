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

	SerialUnitRepositoryAdapter(SerialUnitJpaRepository jpaRepository, SerialUnitPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public List<SerialUnit> saveAll(List<SerialUnit> serialUnits) {
		List<SerialUnitJpaEntity> entities = serialUnits.stream().map(mapper::toEntity).toList();
		entities.forEach(entity -> entity.setNew(!jpaRepository.existsById(entity.getId())));
		return jpaRepository.saveAll(entities).stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public List<SerialUnit> findByProductIdAndWarehouseIdAndSerialNumberIn(UUID productId, UUID warehouseId,
			List<String> serialNumbers) {
		return jpaRepository.findByProductIdAndWarehouseIdAndSerialNumberIn(productId, warehouseId, serialNumbers)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}
}
