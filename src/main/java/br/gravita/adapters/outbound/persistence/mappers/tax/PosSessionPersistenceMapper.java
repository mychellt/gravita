package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PosSessionPersistenceMapper {

	default PosSession toDomain(final PosSessionJpaEntity entity) {
		return PosSession.of(
				PosSessionId.of(entity.getId()),
				entity.getRegisterId(),
				entity.getOperatorId(),
				CompanyId.of(entity.getCompanyId()),
				entity.getOpeningChangeAmount(),
				entity.getStatus(),
				entity.getOpenedAt(),
				entity.getClosedAt());
	}

	default PosSessionJpaEntity toEntity(final PosSession domain) {
		return PosSessionJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.registerId(domain.getRegisterId())
				.operatorId(domain.getOperatorId())
				.companyId(domain.getCompanyId().value())
				.openingChangeAmount(domain.getOpeningChangeAmount())
				.status(domain.getStatus())
				.openedAt(domain.getOpenedAt())
				.closedAt(domain.getClosedAt())
				.build();
	}
}
