package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.Company;
import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.shared.Document;

class CompanyPersistenceMapper {

	Company toDomain(CompanyJpaEntity entity) {
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

	CompanyJpaEntity toEntity(Company domain) {
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
