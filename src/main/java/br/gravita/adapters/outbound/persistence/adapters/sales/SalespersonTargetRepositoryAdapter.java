package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.SalespersonTargetPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalespersonTargetJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class SalespersonTargetRepositoryAdapter implements SalespersonTargetRepositoryPort {

	private final SalespersonTargetJpaRepository jpaRepository;
	private final SalespersonTargetPersistenceMapper mapper;

	SalespersonTargetRepositoryAdapter(final SalespersonTargetJpaRepository jpaRepository,
			final SalespersonTargetPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public SalespersonTarget save(final SalespersonTarget target) {
		final String month = target.month().toString();
		final UUID id = jpaRepository.findBySalespersonIdAndMonth(target.salespersonId(), month)
				.map(SalespersonTargetJpaEntity::getId)
				.orElseGet(UUID::randomUUID);

		final SalespersonTargetJpaEntity entity = mapper.map(target, id);
		entity.setNew(!jpaRepository.existsById(id));
		final SalespersonTargetJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<SalespersonTarget> findBySalespersonAndMonth(final UUID salespersonId, final YearMonth month) {
		return jpaRepository.findBySalespersonIdAndMonth(salespersonId, month.toString()).map(mapper::map);
	}

	@Override
	public List<SalespersonTarget> findByMonth(final YearMonth month) {
		return jpaRepository.findByMonth(month.toString()).stream().map(mapper::map).toList();
	}
}
