package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseRequestJpaEntity;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseRequestPersistenceMapper {

	default PurchaseRequest toDomain(final PurchaseRequestJpaEntity entity) {
		return PurchaseRequest.of(
				PurchaseRequestId.of(entity.getId()),
				entity.getOrigin(),
				toItems(entity.getItems()),
				entity.getRequestedBy(),
				entity.getStatus());
	}

	default PurchaseRequestJpaEntity toEntity(final PurchaseRequest domain) {
		return PurchaseRequestJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.origin(domain.getOrigin())
				.status(domain.getStatus())
				.requestedBy(domain.getRequestedBy())
				.items(toItemEmbeddables(domain.getItems()))
				.build();
	}

	private List<PurchaseRequestItem> toItems(final List<PurchaseRequestItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new PurchaseRequestItem(e.getProductId(), e.getQuantity()))
				.toList();
	}

	private List<PurchaseRequestItemEmbeddable> toItemEmbeddables(final List<PurchaseRequestItem> items) {
		// Hibernate merges a detached entity's collections in place (clear + addAll), so
		// this must stay mutable rather than an immutable Stream.toList().
		return items.stream()
				.map(item -> PurchaseRequestItemEmbeddable.builder()
						.productId(item.productId())
						.quantity(item.quantity())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
