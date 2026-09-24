package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockBalancePersistenceMapper {

	default StockBalance toDomain(final StockBalanceJpaEntity entity) {
		return StockBalance.of(
				StockBalanceId.of(entity.getId()),
				entity.getProductId(),
				entity.getWarehouseId(),
				entity.getOnHand(),
				entity.getReserved(),
				entity.getInTransit(),
				entity.getAverageCost());
	}
}
