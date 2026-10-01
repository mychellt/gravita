package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.util.UUID;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CompanyPersistenceMapper {

    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "toCnpj")
    Company map(final CompanyJpaEntity entity);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "cnpj", source = "cnpj.number")
    @Mapping(target = "parentCompanyId", source = "parentCompanyId.value")
    CompanyJpaEntity map(final Company domain);

    @Mapping(target = "value", source = "id")
    CompanyId map(final UUID id);

    @Named("toCnpj")
    static Document toCnpj(final String cnpj) {
        return Document.cnpj(cnpj);
    }
}
