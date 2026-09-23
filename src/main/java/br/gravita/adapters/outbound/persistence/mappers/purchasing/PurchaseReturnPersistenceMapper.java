package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReturnItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReturnJpaEntity;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.domain.purchasing.PurchaseReturnItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseReturnPersistenceMapper {

	default PurchaseReturn toDomain(final PurchaseReturnJpaEntity entity) {
		return PurchaseReturn.of(
				PurchaseReturnId.of(entity.getId()),
				PurchaseReceiptId.of(entity.getReceiptId()),
				toItems(entity.getItems()),
				entity.isTotal(),
				entity.getReturnNfeRef());
	}

	default PurchaseReturnJpaEntity toEntity(final PurchaseReturn domain) {
		return PurchaseReturnJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.receiptId(domain.getReceiptId().value())
				.total(domain.isTotal())
				.returnNfeRef(domain.getReturnNfeRef())
				.items(toItemEmbeddables(domain.getItems()))
				.build();
	}

	private List<PurchaseReturnItem> toItems(final List<PurchaseReturnItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new PurchaseReturnItem(e.getProductId(), e.getQuantity()))
				.toList();
	}

	// Hibernate merges a detached entity's collections in place (clear + addAll), so
	// these must stay mutable rather than an immutable Stream.toList().
	private List<PurchaseReturnItemEmbeddable> toItemEmbeddables(final List<PurchaseReturnItem> items) {
		return items.stream()
				.map(item -> PurchaseReturnItemEmbeddable.builder()
						.productId(item.productId())
						.quantity(item.quantity())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
