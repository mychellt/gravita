package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.LotJpaEntity;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface LotPersistenceMapper {

	default Lot toDomain(final LotJpaEntity entity) {
		return Lot.of(
				LotId.of(entity.getId()),
				entity.getProductId(),
				entity.getWarehouseId(),
				entity.getCode(),
				entity.getExpiryDate(),
				entity.getQuantity());
	}

	default LotJpaEntity toEntity(final Lot domain) {
		return LotJpaEntity.builder()
				.id(domain.getId().value())
				.productId(domain.getProductId())
				.warehouseId(domain.getWarehouseId())
				.code(domain.getCode())
				.expiryDate(domain.getExpiryDate())
				.quantity(domain.getQuantity())
				.build();
	}
}
