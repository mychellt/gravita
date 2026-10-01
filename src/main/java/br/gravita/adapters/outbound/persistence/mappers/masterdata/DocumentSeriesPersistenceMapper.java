package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DocumentSeriesPersistenceMapper {

    DocumentSeries map(final DocumentSeriesJpaEntity entity);

    @Mapping(target = "companyId", source = "companyId.value")
    DocumentSeriesJpaEntity map(final DocumentSeries domain);

    @Mapping(target = "value", source = "id")
    CompanyId map(final UUID id);
}
