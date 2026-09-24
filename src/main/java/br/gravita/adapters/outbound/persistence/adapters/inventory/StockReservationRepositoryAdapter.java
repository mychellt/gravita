package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockReservationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockReservationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class StockReservationRepositoryAdapter implements StockReservationRepositoryPort {

	private final StockReservationJpaRepository jpaRepository;
	private final StockReservationPersistenceMapper mapper;

	StockReservationRepositoryAdapter(StockReservationJpaRepository jpaRepository,
			StockReservationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public StockReservation save(StockReservation reservation) {
		StockReservationJpaEntity entity = mapper.toEntity(reservation);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<StockReservation> findById(StockReservationId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
