package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockTransferJpaEntity;
import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface StockTransferPersistenceMapper {

	default StockTransfer toDomain(final StockTransferJpaEntity entity) {
		return StockTransfer.of(
				StockTransferId.of(entity.getId()),
				entity.getProductId(),
				entity.getSourceWarehouseId(),
				entity.getDestinationWarehouseId(),
				entity.getQuantity(),
				entity.getStatus());
	}

	default StockTransferJpaEntity toEntity(final StockTransfer domain) {
		return StockTransferJpaEntity.builder()
				.id(domain.getId().value())
				.productId(domain.getProductId())
				.sourceWarehouseId(domain.getSourceWarehouseId())
				.destinationWarehouseId(domain.getDestinationWarehouseId())
				.quantity(domain.getQuantity())
				.status(domain.getStatus())
				.build();
	}
}
