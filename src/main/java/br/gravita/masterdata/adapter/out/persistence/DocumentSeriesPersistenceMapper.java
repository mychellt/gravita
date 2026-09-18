package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.masterdata.domain.model.DocumentSeries;

class DocumentSeriesPersistenceMapper {

	DocumentSeries toDomain(DocumentSeriesJpaEntity entity) {
		return DocumentSeries.of(
				entity.getId(),
				CompanyId.of(entity.getCompanyId()),
				entity.getDocumentType(),
				entity.getSeries(),
				entity.getNextNumber());
	}

	DocumentSeriesJpaEntity toEntity(DocumentSeries domain) {
		return DocumentSeriesJpaEntity.builder()
				.id(domain.getId())
				.companyId(domain.getCompanyId().value())
				.documentType(domain.getDocumentType())
				.series(domain.getSeries())
				.nextNumber(domain.getNextNumber())
				.build();
	}
}
