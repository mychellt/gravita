package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.mappers.inventory.SerialUnitPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.SerialUnitJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import java.util.List;

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
		return jpaRepository.saveAll(serialUnits.stream().map(mapper::toEntity).toList()).stream()
				.map(mapper::toDomain)
				.toList();
	}
}
