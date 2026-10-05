package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.DocumentSeriesJpaRepository;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.annotations.PersistenceAdapter;

import java.util.Optional;

@PersistenceAdapter
class DocumentSeriesRepositoryAdapter implements DocumentSeriesRepositoryPort {

	private final DocumentSeriesJpaRepository jpaRepository;
	private final DocumentSeriesPersistenceMapper mapper;

	DocumentSeriesRepositoryAdapter(final DocumentSeriesJpaRepository jpaRepository, final DocumentSeriesPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public DocumentSeries save(final DocumentSeries documentSeries) {
		final DocumentSeriesJpaEntity entity = mapper.map(documentSeries);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final DocumentSeriesJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<DocumentSeries> findByCompanyIdAndDocumentType(final CompanyId companyId, final FiscalDocumentType documentType) {
		return jpaRepository.findByCompanyIdAndDocumentType(companyId.value(), documentType).map(entity -> mapper.map(entity));
	}
}
