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

	VoidedNumberRangeRepositoryAdapter(VoidedNumberRangeJpaRepository jpaRepository,
			VoidedNumberRangePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public VoidedNumberRange save(VoidedNumberRange voidedNumberRange) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(voidedNumberRange)));
	}

	@Override
	public List<VoidedNumberRange> findByCompanyIdAndSeriesAndDateRange(CompanyId companyId, String series,
			Instant dateFrom, Instant dateTo) {
		return jpaRepository.search(companyId.value(), series, dateFrom, dateTo).stream().map(mapper::toDomain)
				.toList();
	}
}
