package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
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
	public SalespersonTarget save(SalespersonTarget target) {
		String month = target.month().toString();
		UUID id = jpaRepository.findBySalespersonIdAndMonth(target.salespersonId(), month)
				.map(SalespersonTargetJpaEntity::getId)
				.orElseGet(UUID::randomUUID);

		SalespersonTargetJpaEntity entity = mapper.toEntity(target, id);
		entity.setNew(!jpaRepository.existsById(id));
		SalespersonTargetJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<SalespersonTarget> findBySalespersonAndMonth(UUID salespersonId, YearMonth month) {
		return jpaRepository.findBySalespersonIdAndMonth(salespersonId, month.toString()).map(mapper::toDomain);
	}
}
