package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import java.util.UUID;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockBalancePersistenceMapper {

	StockBalance map(final StockBalanceJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	StockBalanceJpaEntity map(final StockBalance domain);

	@Mapping(target = "value", source = "id")
	StockBalanceId mapStockBalanceId(final UUID id);
}
