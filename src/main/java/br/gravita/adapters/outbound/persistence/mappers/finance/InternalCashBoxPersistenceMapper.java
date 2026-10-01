package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashBoxJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashMovementJpaEntity;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InternalCashBoxPersistenceMapper {

	InternalCashBox map(final InternalCashBoxJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	InternalCashBoxJpaEntity map(final InternalCashBox domain);

	CashMovement map(final InternalCashMovementJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "cashBoxId", source = "cashBoxId.value")
	InternalCashMovementJpaEntity map(final CashMovement domain);

	@Mapping(target = "value", source = "id")
	InternalCashBoxId mapInternalCashBoxId(final UUID id);

	@Mapping(target = "value", source = "id")
	CashMovementId mapCashMovementId(final UUID id);
}
