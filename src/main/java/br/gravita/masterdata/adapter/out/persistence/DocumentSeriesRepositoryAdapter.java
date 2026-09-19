package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.application.port.out.DocumentSeriesRepositoryPort;
import br.gravita.masterdata.domain.model.DocumentSeries;
import br.gravita.shared.PersistenceAdapter;

@PersistenceAdapter
class DocumentSeriesRepositoryAdapter implements DocumentSeriesRepositoryPort {

	private final DocumentSeriesJpaRepository jpaRepository;
	private final DocumentSeriesPersistenceMapper mapper = new DocumentSeriesPersistenceMapper();

	DocumentSeriesRepositoryAdapter(DocumentSeriesJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DocumentSeries save(DocumentSeries documentSeries) {
		DocumentSeriesJpaEntity entity = mapper.toEntity(documentSeries);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		DocumentSeriesJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
