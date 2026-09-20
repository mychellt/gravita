package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.DocumentSeriesJpaRepository;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.annotations.PersistenceAdapter;

@PersistenceAdapter
class DocumentSeriesRepositoryAdapter implements DocumentSeriesRepositoryPort {

	private final DocumentSeriesJpaRepository jpaRepository;
	private final DocumentSeriesPersistenceMapper mapper;

	DocumentSeriesRepositoryAdapter(DocumentSeriesJpaRepository jpaRepository, DocumentSeriesPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public DocumentSeries save(DocumentSeries documentSeries) {
		DocumentSeriesJpaEntity entity = mapper.toEntity(documentSeries);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		DocumentSeriesJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
