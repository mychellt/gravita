package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.LotJpaEntity;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface LotPersistenceMapper {

	Lot map(final LotJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	LotJpaEntity map(final Lot domain);

	@Mapping(target = "value", source = "id")
	LotId mapLotId(final UUID id);
}
