package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockReservationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockReservationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class StockReservationRepositoryAdapter implements StockReservationRepositoryPort {

	private final StockReservationJpaRepository jpaRepository;
	private final StockReservationPersistenceMapper mapper;

	StockReservationRepositoryAdapter(final StockReservationJpaRepository jpaRepository,
			final StockReservationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public StockReservation save(final StockReservation reservation) {
		final StockReservationJpaEntity entity = mapper.map(reservation);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public Optional<StockReservation> findById(final StockReservationId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<StockReservation> findActiveByOrderRef(final UUID orderRef) {
		return jpaRepository.findByOrderRefAndStatus(orderRef, StockReservationStatus.ACTIVE).stream()
				.map(mapper::map)
				.toList();
	}
}
