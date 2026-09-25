package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockMovementJpaEntity;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockMovementPersistenceMapper {

	default StockMovement toDomain(final StockMovementJpaEntity entity) {
		return StockMovement.of(
				StockMovementId.of(entity.getId()),
				entity.getType(),
				entity.getProductId(),
				entity.getWarehouseId(),
				entity.getQuantity(),
				entity.getUnitCost(),
				entity.getLotCode(),
				entity.getSerialNumbers(),
				entity.getOriginReference(),
				entity.getJustification(),
				entity.getUser(),
				entity.getTimestamp());
	}

	default StockMovementJpaEntity toEntity(final StockMovement domain) {
		return StockMovementJpaEntity.builder()
				.id(domain.getId().value())
				.type(domain.getType())
				.productId(domain.getProductId())
				.warehouseId(domain.getWarehouseId())
				.quantity(domain.getQuantity())
				.unitCost(domain.getUnitCost())
				.lotCode(domain.getLotCode())
				.serialNumbers(domain.getSerialNumbers())
				.originReference(domain.getOriginReference())
				.justification(domain.getJustification())
				.user(domain.getUser())
				.timestamp(domain.getTimestamp())
				.build();
	}
}
