package br.gravita.adapters.outbound.persistence.mappers.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import org.mapstruct.Mapper;

@Mapper
public interface CompanyPersistenceMapper {

    default Company toDomain(final CompanyJpaEntity entity) {
        return Company.of(
                CompanyId.of(entity.getId()),
                Document.cnpj(entity.getCnpj()),
                entity.getIe(),
                entity.getIm(),
                entity.getCnae(),
                entity.getTaxRegime(),
                entity.isSimplesOptante(),
                entity.getSefazEnvironment(),
                entity.getAddress(),
                entity.getIssuingEmail(),
                entity.getPhone(),
                entity.getLogoUrl(),
                entity.getParentCompanyId() == null ? null : CompanyId.of(entity.getParentCompanyId()));
    }

    default CompanyJpaEntity toEntity(final Company domain) {
        return CompanyJpaEntity.builder()
                .id(domain.getId() == null ? null : domain.getId().value())
                .cnpj(domain.getCnpj().number())
                .ie(domain.getIe())
                .im(domain.getIm())
                .cnae(domain.getCnae())
                .taxRegime(domain.getTaxRegime())
                .simplesOptante(domain.isSimplesOptante())
                .sefazEnvironment(domain.getSefazEnvironment())
                .address(domain.getAddress())
                .issuingEmail(domain.getIssuingEmail())
                .phone(domain.getPhone())
                .logoUrl(domain.getLogoUrl())
                .parentCompanyId(domain.getParentCompanyId() == null ? null : domain.getParentCompanyId().value())
                .build();
    }
}
