package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.mappers.sales.SalespersonTargetPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalespersonTargetJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class SalespersonTargetRepositoryAdapter implements SalespersonTargetRepositoryPort {

	private final SalespersonTargetJpaRepository jpaRepository;
	private final SalespersonTargetPersistenceMapper mapper;

	SalespersonTargetRepositoryAdapter(SalespersonTargetJpaRepository jpaRepository,
			SalespersonTargetPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<SalespersonTarget> findBySalespersonAndMonth(UUID salespersonId, YearMonth month) {
		return jpaRepository.findBySalespersonIdAndReferenceMonth(salespersonId, month.atDay(1)).map(mapper::toDomain);
	}
}
