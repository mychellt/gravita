package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashClosingReportJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.CashClosingReportPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.CashClosingReportJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.outbound.persistence.tax.CashClosingReportRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class CashClosingReportRepositoryAdapter implements CashClosingReportRepositoryPort {

	private final CashClosingReportJpaRepository jpaRepository;
	private final CashClosingReportPersistenceMapper mapper;

	CashClosingReportRepositoryAdapter(final CashClosingReportJpaRepository jpaRepository,
			final CashClosingReportPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public CashClosingReport save(final CashClosingReport report) {
		final CashClosingReportJpaEntity entity = mapper.map(report);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final CashClosingReportJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<CashClosingReport> findBySessionId(final PosSessionId sessionId) {
		return jpaRepository.findBySessionId(sessionId.value()).map(mapper::map);
	}
}
