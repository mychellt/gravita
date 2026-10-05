package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.mappers.inventory.StockMovementPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockMovementJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import java.time.Instant;
import java.util.List;

@PersistenceAdapter
class StockMovementRepositoryAdapter implements StockMovementRepositoryPort {

	private final StockMovementJpaRepository jpaRepository;
	private final StockMovementPersistenceMapper mapper;

	StockMovementRepositoryAdapter(final StockMovementJpaRepository jpaRepository, final StockMovementPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public StockMovement save(final StockMovement movement) {
		return mapper.map(jpaRepository.save(mapper.map(movement)));
	}

	@Override
	public List<StockMovement> findByTimestampGreaterThanEqual(final Instant from) {
		return jpaRepository.findByTimestampGreaterThanEqual(from).stream().map(mapper::map).toList();
	}
}
