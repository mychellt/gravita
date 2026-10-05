package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.VoidedNumberRangePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.VoidedNumberRangeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import java.time.Instant;
import java.util.List;

@PersistenceAdapter
class VoidedNumberRangeRepositoryAdapter implements VoidedNumberRangeRepositoryPort {

	private final VoidedNumberRangeJpaRepository jpaRepository;
	private final VoidedNumberRangePersistenceMapper mapper;

	VoidedNumberRangeRepositoryAdapter(final VoidedNumberRangeJpaRepository jpaRepository,
			final VoidedNumberRangePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public VoidedNumberRange save(final VoidedNumberRange voidedNumberRange) {
		return mapper.map(jpaRepository.save(mapper.map(voidedNumberRange)));
	}

	@Override
	public List<VoidedNumberRange> findByCompanyId(final CompanyId companyId) {
		return jpaRepository.findByCompanyId(companyId.value()).stream().map(mapper::map).toList();
	}

	@Override
	public List<VoidedNumberRange> findByCompanyIdAndSeriesAndVoidedAtBetween(final CompanyId companyId, final String series,
			final Instant voidedFrom, final Instant voidedTo) {
		return jpaRepository.findByCompanyIdAndSeriesAndVoidedAtBetween(companyId.value(), series, voidedFrom, voidedTo)
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<VoidedNumberRange> findByCompanyIdAndVoidedAtBetween(final CompanyId companyId, final Instant voidedFrom,
			final Instant voidedTo) {
		return jpaRepository
				.findByCompanyIdAndVoidedAtGreaterThanEqualAndVoidedAtLessThanOrderByVoidedAtAscSeriesAscStartNumberAsc(
						companyId.value(), voidedFrom, voidedTo)
				.stream().map(mapper::map).toList();
	}
}
