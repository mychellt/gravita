package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashBoxJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashMovementJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.finance.InternalCashBoxPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.finance.InternalCashBoxJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.finance.InternalCashMovementJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class InternalCashBoxRepositoryAdapter implements InternalCashBoxRepositoryPort {

	private final InternalCashBoxJpaRepository cashBoxJpaRepository;
	private final InternalCashMovementJpaRepository movementJpaRepository;
	private final InternalCashBoxPersistenceMapper mapper;

	InternalCashBoxRepositoryAdapter(InternalCashBoxJpaRepository cashBoxJpaRepository,
			InternalCashMovementJpaRepository movementJpaRepository, InternalCashBoxPersistenceMapper mapper) {
		this.cashBoxJpaRepository = cashBoxJpaRepository;
		this.movementJpaRepository = movementJpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<InternalCashBox> findById(InternalCashBoxId id) {
		return cashBoxJpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public Optional<InternalCashBox> findByIdForUpdate(InternalCashBoxId id) {
		return cashBoxJpaRepository.findByIdForUpdate(id.value()).map(mapper::map);
	}

	@Override
	public InternalCashBox save(InternalCashBox cashBox) {
		InternalCashBoxJpaEntity entity = mapper.map(cashBox);
		entity.setNew(!cashBoxJpaRepository.existsById(entity.getId()));
		return mapper.map(cashBoxJpaRepository.save(entity));
	}

	@Override
	public CashMovement saveMovement(CashMovement movement) {
		InternalCashMovementJpaEntity entity = mapper.map(movement);
		entity.setNew(!movementJpaRepository.existsById(entity.getId()));
		return mapper.map(movementJpaRepository.save(entity));
	}

	@Override
	public List<CashMovement> findMovementsFrom(InternalCashBoxId cashBoxId, Instant from) {
		return movementJpaRepository
				.findByCashBoxIdAndTimestampGreaterThanEqualOrderByTimestampAsc(cashBoxId.value(), from)
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<CashMovement> findMovementsBetween(Instant from, Instant until) {
		return movementJpaRepository
				.findByTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAscIdAsc(from, until).stream()
				.map(mapper::map).toList();
	}
}
