package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.PhysicalCountPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.PhysicalCountJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsQuery;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
class PhysicalCountRepositoryAdapter implements PhysicalCountRepositoryPort {

	private final PhysicalCountJpaRepository jpaRepository;
	private final PhysicalCountPersistenceMapper mapper;

	PhysicalCountRepositoryAdapter(final PhysicalCountJpaRepository jpaRepository, final PhysicalCountPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PhysicalCount save(final PhysicalCount physicalCount) {
		final PhysicalCountJpaEntity entity = mapper.map(physicalCount);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final PhysicalCountJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PhysicalCount> findById(final PhysicalCountId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PhysicalCount> search(final UUID warehouseId, final ListPhysicalCountsQuery query) {
		final Sort newestFirst = Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"));
		return PageMapping.toPage(jpaRepository.search(warehouseId, query.status(),
				PageMapping.pageable(query.pageQuery(), newestFirst)), mapper::map);
	}
}
