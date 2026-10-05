package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.CashMovementPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.CashMovementJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.outbound.persistence.tax.CashMovementRepositoryPort;
import java.util.List;

@PersistenceAdapter
class CashMovementRepositoryAdapter implements CashMovementRepositoryPort {

	private final CashMovementJpaRepository jpaRepository;
	private final CashMovementPersistenceMapper mapper;

	CashMovementRepositoryAdapter(final CashMovementJpaRepository jpaRepository, final CashMovementPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public CashMovement save(final CashMovement cashMovement) {
		return mapper.map(jpaRepository.save(mapper.map(cashMovement)));
	}

	@Override
	public List<CashMovement> findBySessionId(final PosSessionId sessionId) {
		return jpaRepository.findBySessionId(sessionId.value()).stream().map(mapper::map).toList();
	}
}
