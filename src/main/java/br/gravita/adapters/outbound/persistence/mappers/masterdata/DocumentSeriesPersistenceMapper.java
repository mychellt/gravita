package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DocumentSeriesPersistenceMapper {

    default DocumentSeries toDomain(final DocumentSeriesJpaEntity entity) {
        return DocumentSeries.of(
                entity.getId(),
                CompanyId.of(entity.getCompanyId()),
                entity.getDocumentType(),
                entity.getSeries(),
                entity.getNextNumber());
    }

    default DocumentSeriesJpaEntity toEntity(final DocumentSeries domain) {
        return DocumentSeriesJpaEntity.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId().value())
                .documentType(domain.getDocumentType())
                .series(domain.getSeries())
                .nextNumber(domain.getNextNumber())
                .build();
    }
}
