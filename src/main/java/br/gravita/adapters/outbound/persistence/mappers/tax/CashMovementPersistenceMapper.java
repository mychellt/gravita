package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.CashMovementJpaEntity;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.domain.tax.PosSessionId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CashMovementPersistenceMapper {

	CashMovement map(final CashMovementJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "sessionId", source = "sessionId.value")
	CashMovementJpaEntity map(final CashMovement domain);

	@Mapping(target = "value", source = "id")
	CashMovementId mapCashMovementId(final UUID id);

	@Mapping(target = "value", source = "id")
	PosSessionId mapPosSessionId(final UUID id);
}
