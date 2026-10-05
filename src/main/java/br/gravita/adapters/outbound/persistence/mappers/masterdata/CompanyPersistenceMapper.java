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

    /** Hand-written so a draft company (fiscal profile not filled in yet) can be loaded without tripping the strict rules. */
    default Company map(final CompanyJpaEntity entity) {
        return Company.rehydrate(CompanyId.of(entity.getId()), entity.getName(), Document.cnpj(entity.getDocument()),
                entity.getIe(), entity.getIm(), entity.getCnae(), entity.getTaxRegime(), entity.isSimplesOptante(),
                entity.getSefazEnvironment(), entity.getAddress(), entity.getState(), entity.getIssuingEmail(),
                entity.getPhone(), entity.getLogoUrl(),
                entity.getParentCompanyId() == null ? null : CompanyId.of(entity.getParentCompanyId()));
    }

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "document", source = "cnpj.number")
    @Mapping(target = "parentCompanyId", source = "parentCompanyId.value")
    CompanyJpaEntity map(final Company domain);

    @Mapping(target = "value", source = "id")
    CompanyId map(final UUID id);

    @Named("toCnpj")
    static Document toCnpj(final String cnpj) {
        return Document.cnpj(cnpj);
    }
}
