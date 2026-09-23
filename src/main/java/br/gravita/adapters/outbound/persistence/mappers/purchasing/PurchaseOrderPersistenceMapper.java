package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderJpaEntity;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseOrderPersistenceMapper {

	default PurchaseOrder toDomain(final PurchaseOrderJpaEntity entity) {
		return PurchaseOrder.of(
				PurchaseOrderId.of(entity.getId()),
				PurchaseRequestId.of(entity.getRequestId()),
				entity.getQuotationId(),
				SupplierId.of(entity.getSupplierId()),
				toItems(entity.getItems()),
				entity.isApprovalRequired(),
				entity.getStatus());
	}

	default PurchaseOrderJpaEntity toEntity(final PurchaseOrder domain) {
		return PurchaseOrderJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.requestId(domain.getRequestId().value())
				.quotationId(domain.getQuotationId())
				.supplierId(domain.getSupplierId().value())
				.approvalRequired(domain.isApprovalRequired())
				.status(domain.getStatus())
				.items(toItemEmbeddables(domain.getItems()))
				.build();
	}

	private List<PurchaseOrderItem> toItems(final List<PurchaseOrderItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new PurchaseOrderItem(e.getProductId(), e.getQuantity(), e.getUnitPrice()))
				.toList();
	}

	private List<PurchaseOrderItemEmbeddable> toItemEmbeddables(final List<PurchaseOrderItem> items) {
		// Hibernate merges a detached entity's collections in place (clear + addAll), so
		// this must stay mutable rather than an immutable Stream.toList().
		return items.stream()
				.map(item -> PurchaseOrderItemEmbeddable.builder()
						.productId(item.productId())
						.quantity(item.quantity())
						.unitPrice(item.unitPrice())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
