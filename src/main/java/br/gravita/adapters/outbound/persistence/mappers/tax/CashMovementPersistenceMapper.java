package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashMovementJpaEntity;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.domain.tax.PosSessionId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CashMovementPersistenceMapper {

	default CashMovement toDomain(final CashMovementJpaEntity entity) {
		return CashMovement.of(
				CashMovementId.of(entity.getId()),
				PosSessionId.of(entity.getSessionId()),
				entity.getType(),
				entity.getAmount(),
				entity.getJustification(),
				entity.getTimestamp());
	}

	default CashMovementJpaEntity toEntity(final CashMovement domain) {
		return CashMovementJpaEntity.builder()
				.id(domain.getId().value())
				.sessionId(domain.getSessionId().value())
				.type(domain.getType())
				.amount(domain.getAmount())
				.justification(domain.getJustification())
				.timestamp(domain.getTimestamp())
				.build();
	}
}
