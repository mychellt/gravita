package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.InstallmentTermEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseReceiptJpaEntity;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PurchaseReceiptPersistenceMapper {

	default PurchaseReceipt toDomain(final PurchaseReceiptJpaEntity entity) {
		return PurchaseReceipt.of(
				PurchaseReceiptId.of(entity.getId()),
				PurchaseOrderId.of(entity.getOrderId()),
				toItems(entity.getItems()),
				toInstallmentTerms(entity.getInstallments()),
				entity.getStatus());
	}

	default PurchaseReceiptJpaEntity toEntity(final PurchaseReceipt domain) {
		return PurchaseReceiptJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.orderId(domain.getOrderId().value())
				.status(domain.getStatus())
				.items(toItemEmbeddables(domain.getReceivedItems()))
				.installments(toInstallmentEmbeddables(domain.getInstallmentTerms()))
				.build();
	}

	private List<PurchaseReceiptItem> toItems(final List<PurchaseReceiptItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new PurchaseReceiptItem(e.getProductId(), e.getOrderedQty(), e.getReceivedQty()))
				.toList();
	}

	private List<PurchaseReceiptItemEmbeddable> toItemEmbeddables(final List<PurchaseReceiptItem> items) {
		return items.stream()
				.map(item -> PurchaseReceiptItemEmbeddable.builder()
						.productId(item.productId())
						.orderedQty(item.orderedQty())
						.receivedQty(item.receivedQty())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private List<InstallmentTerm> toInstallmentTerms(final List<InstallmentTermEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new InstallmentTerm(e.getAmount(), e.getDueDate()))
				.toList();
	}

	private List<InstallmentTermEmbeddable> toInstallmentEmbeddables(final List<InstallmentTerm> installmentTerms) {
		return installmentTerms.stream()
				.map(term -> InstallmentTermEmbeddable.builder()
						.amount(term.amount())
						.dueDate(term.dueDate())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
