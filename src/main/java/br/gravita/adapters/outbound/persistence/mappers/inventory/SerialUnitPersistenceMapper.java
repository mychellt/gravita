package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.SerialUnitJpaEntity;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.inventory.SerialUnitId;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SerialUnitPersistenceMapper {

	default SerialUnit toDomain(final SerialUnitJpaEntity entity) {
		return SerialUnit.received(
				SerialUnitId.of(entity.getId()),
				entity.getProductId(),
				entity.getWarehouseId(),
				entity.getSerialNumber());
	}

	default SerialUnitJpaEntity toEntity(final SerialUnit domain) {
		return SerialUnitJpaEntity.builder()
				.id(domain.getId().value())
				.productId(domain.getProductId())
				.warehouseId(domain.getWarehouseId())
				.serialNumber(domain.getSerialNumber())
				.status(domain.getStatus())
				.build();
	}
}
