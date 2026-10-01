package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.SettlementJpaEntity;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SettlementPersistenceMapper {

	Settlement map(final SettlementJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "receivableId", source = "receivableId.value")
	@Mapping(target = "payableId", source = "payableId.value")
	SettlementJpaEntity map(final Settlement domain);

	@Mapping(target = "value", source = "id")
	SettlementId mapSettlementId(final UUID id);

	@Mapping(target = "value", source = "id")
	ReceivableId mapReceivableId(final UUID id);

	@Mapping(target = "value", source = "id")
	PayableId mapPayableId(final UUID id);
}
