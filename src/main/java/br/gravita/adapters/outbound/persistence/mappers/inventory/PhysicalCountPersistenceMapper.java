package br.gravita.adapters.outbound.persistence.mappers.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountLineEmbeddable;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PhysicalCountPersistenceMapper {

	default PhysicalCount toDomain(final PhysicalCountJpaEntity entity) {
		return PhysicalCount.of(
				PhysicalCountId.of(entity.getId()),
				entity.getScope(),
				entity.getProductGroupId(),
				entity.getWarehouseId(),
				entity.getStatus(),
				entity.getStartedBy(),
				entity.getStartedAt(),
				toLines(entity.getLines()));
	}

	default PhysicalCountJpaEntity toEntity(final PhysicalCount domain) {
		return PhysicalCountJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.scope(domain.getScope())
				.productGroupId(domain.getProductGroupId())
				.warehouseId(domain.getWarehouseId())
				.status(domain.getStatus())
				.startedBy(domain.getStartedBy())
				.startedAt(domain.getStartedAt())
				.lines(toLineEmbeddables(domain.getLines()))
				.build();
	}

	private List<PhysicalCountLine> toLines(final List<PhysicalCountLineEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new PhysicalCountLine(e.getProductId(), e.getSystemQuantity(), e.getCountedQuantity()))
				.toList();
	}

	private List<PhysicalCountLineEmbeddable> toLineEmbeddables(final List<PhysicalCountLine> lines) {
		return lines.stream()
				.map(line -> PhysicalCountLineEmbeddable.builder()
						.productId(line.productId())
						.systemQuantity(line.systemQuantity())
						.countedQuantity(line.countedQuantity())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
