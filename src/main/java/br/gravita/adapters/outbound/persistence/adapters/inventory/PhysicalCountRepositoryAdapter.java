package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.PhysicalCountPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.PhysicalCountJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class PhysicalCountRepositoryAdapter implements PhysicalCountRepositoryPort {

	private final PhysicalCountJpaRepository jpaRepository;
	private final PhysicalCountPersistenceMapper mapper;

	PhysicalCountRepositoryAdapter(PhysicalCountJpaRepository jpaRepository, PhysicalCountPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PhysicalCount save(PhysicalCount physicalCount) {
		PhysicalCountJpaEntity entity = mapper.map(physicalCount);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PhysicalCountJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PhysicalCount> findById(PhysicalCountId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}
}
