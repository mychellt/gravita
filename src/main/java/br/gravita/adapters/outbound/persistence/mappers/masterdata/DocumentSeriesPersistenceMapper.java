package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;

public class DocumentSeriesPersistenceMapper {

	public DocumentSeries toDomain(DocumentSeriesJpaEntity entity) {
		return DocumentSeries.of(
				entity.getId(),
				CompanyId.of(entity.getCompanyId()),
				entity.getDocumentType(),
				entity.getSeries(),
				entity.getNextNumber());
	}

	public DocumentSeriesJpaEntity toEntity(DocumentSeries domain) {
		return DocumentSeriesJpaEntity.builder()
				.id(domain.getId())
				.companyId(domain.getCompanyId().value())
				.documentType(domain.getDocumentType())
				.series(domain.getSeries())
				.nextNumber(domain.getNextNumber())
				.build();
	}
}
