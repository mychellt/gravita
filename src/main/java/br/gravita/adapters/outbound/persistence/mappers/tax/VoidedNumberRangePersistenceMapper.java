package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.VoidedNumberRangeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface VoidedNumberRangePersistenceMapper {

	default VoidedNumberRange toDomain(final VoidedNumberRangeJpaEntity entity) {
		return VoidedNumberRange.of(
				VoidedNumberRangeId.of(entity.getId()),
				CompanyId.of(entity.getCompanyId()),
				entity.getDocumentType(),
				entity.getSeries(),
				entity.getStartNumber(),
				entity.getEndNumber(),
				entity.getJustification(),
				entity.getSefazProtocol(),
				entity.getVoidedAt());
	}

	default VoidedNumberRangeJpaEntity toEntity(final VoidedNumberRange domain) {
		return VoidedNumberRangeJpaEntity.builder()
				.id(domain.getId().value())
				.companyId(domain.getCompanyId().value())
				.documentType(domain.getDocumentType())
				.series(domain.getSeries())
				.startNumber(domain.getStartNumber())
				.endNumber(domain.getEndNumber())
				.justification(domain.getJustification())
				.sefazProtocol(domain.getSefazProtocol())
				.voidedAt(domain.getVoidedAt())
				.build();
	}
}
