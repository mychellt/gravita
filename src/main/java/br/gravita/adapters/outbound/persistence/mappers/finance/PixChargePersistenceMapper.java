package br.gravita.adapters.outbound.persistence.mappers.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.PixChargeJpaEntity;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.ReceivableId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PixChargePersistenceMapper {

	PixCharge map(final PixChargeJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "receivableId", source = "receivableId.value")
	PixChargeJpaEntity map(final PixCharge domain);

	@Mapping(target = "value", source = "id")
	PixChargeId mapPixChargeId(final UUID id);

	@Mapping(target = "value", source = "id")
	ReceivableId mapReceivableId(final UUID id);
}
