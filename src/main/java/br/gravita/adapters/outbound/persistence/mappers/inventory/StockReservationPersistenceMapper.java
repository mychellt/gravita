package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockReservationPersistenceMapper {

	default StockReservation toDomain(final StockReservationJpaEntity entity) {
		return StockReservation.of(
				StockReservationId.of(entity.getId()),
				entity.getOrderRef(),
				entity.getProductId(),
				entity.getWarehouseId(),
				entity.getQuantity(),
				entity.getStatus());
	}

	default StockReservationJpaEntity toEntity(final StockReservation domain) {
		return StockReservationJpaEntity.builder()
				.id(domain.getId().value())
				.orderRef(domain.getOrderRef())
				.productId(domain.getProductId())
				.warehouseId(domain.getWarehouseId())
				.quantity(domain.getQuantity())
				.status(domain.getStatus())
				.build();
	}
}
