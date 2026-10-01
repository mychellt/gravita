package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockMovementJpaEntity;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockMovementPersistenceMapper {

	StockMovement map(final StockMovementJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	StockMovementJpaEntity map(final StockMovement domain);

	@Mapping(target = "value", source = "id")
	StockMovementId mapStockMovementId(final UUID id);
}
