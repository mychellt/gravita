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
		return cashBoxJpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public Optional<InternalCashBox> findByIdForUpdate(InternalCashBoxId id) {
		return cashBoxJpaRepository.findByIdForUpdate(id.value()).map(mapper::toDomain);
	}

	@Override
	public InternalCashBox save(InternalCashBox cashBox) {
		InternalCashBoxJpaEntity entity = mapper.toEntity(cashBox);
		entity.setNew(!cashBoxJpaRepository.existsById(entity.getId()));
		return mapper.toDomain(cashBoxJpaRepository.save(entity));
	}

	@Override
	public CashMovement saveMovement(CashMovement movement) {
		InternalCashMovementJpaEntity entity = mapper.toEntity(movement);
		entity.setNew(!movementJpaRepository.existsById(entity.getId()));
		return mapper.toDomain(movementJpaRepository.save(entity));
	}
}
