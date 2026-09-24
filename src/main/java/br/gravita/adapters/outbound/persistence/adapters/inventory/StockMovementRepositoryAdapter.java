package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.mappers.inventory.StockMovementPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockMovementJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;

@PersistenceAdapter
class StockMovementRepositoryAdapter implements StockMovementRepositoryPort {

	private final StockMovementJpaRepository jpaRepository;
	private final StockMovementPersistenceMapper mapper;

	StockMovementRepositoryAdapter(StockMovementJpaRepository jpaRepository, StockMovementPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public StockMovement save(StockMovement movement) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(movement)));
	}
}
