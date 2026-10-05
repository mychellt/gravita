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
        return Company.rehydrate()
        		.id(CompanyId.of(entity.getId()))
        		.name(entity.getName())
        		.cnpj(Document.cnpj(entity.getDocument()))
        		.ie(entity.getIe())
        		.im(entity.getIm())
        		.cnae(entity.getCnae())
        		.taxRegime(entity.getTaxRegime())
        		.simplesOptante(entity.isSimplesOptante())
        		.sefazEnvironment(entity.getSefazEnvironment())
        		.address(entity.getAddress())
        		.state(entity.getState())
        		.issuingEmail(entity.getIssuingEmail())
        		.phone(entity.getPhone())
        		.logoUrl(entity.getLogoUrl())
        		.parentCompanyId(entity.getParentCompanyId() == null ? null : CompanyId.of(entity.getParentCompanyId()))
        		.build();
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
