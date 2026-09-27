package br.gravita.adapters.outbound.persistence.mappers.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationResponseLineEmbeddable;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItem;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import br.gravita.core.domain.purchasing.QuotationResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface QuotationPersistenceMapper {

	default Quotation toDomain(final QuotationJpaEntity entity) {
		return Quotation.of(
				QuotationId.of(entity.getId()),
				PurchaseRequestId.of(entity.getRequestId()),
				toItems(entity.getItems()),
				toSuppliers(entity.getSuppliers()),
				toResponses(entity.getResponseLines()));
	}

	default QuotationJpaEntity toEntity(final Quotation domain) {
		return QuotationJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.requestId(domain.getRequestId().value())
				.items(toItemEmbeddables(domain.getItems()))
				.suppliers(toSupplierIds(domain.getSuppliers()))
				.responseLines(toResponseLineEmbeddables(domain.getResponses()))
				.build();
	}

	private List<QuotationItem> toItems(final List<QuotationItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream().map(e -> new QuotationItem(e.getProductId(), e.getQuantity())).toList();
	}

	private List<QuotationItemEmbeddable> toItemEmbeddables(final List<QuotationItem> items) {
		return items.stream()
				.map(item -> QuotationItemEmbeddable.builder()
						.productId(item.productId())
						.quantity(item.quantity())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private List<SupplierId> toSuppliers(final List<UUID> supplierIds) {
		if (supplierIds == null) {
			return List.of();
		}
		return supplierIds.stream().map(SupplierId::of).toList();
	}

	private List<UUID> toSupplierIds(final List<SupplierId> suppliers) {
		return suppliers.stream().map(SupplierId::value).collect(Collectors.toCollection(ArrayList::new));
	}

	private List<QuotationResponse> toResponses(final List<QuotationResponseLineEmbeddable> lines) {
		if (lines == null || lines.isEmpty()) {
			return List.of();
		}
		Map<UUID, List<QuotationResponseLineEmbeddable>> linesBySupplier = lines.stream()
				.collect(Collectors.groupingBy(QuotationResponseLineEmbeddable::getSupplierId, LinkedHashMap::new,
						Collectors.toList()));
		return linesBySupplier.entrySet().stream()
				.map(entry -> new QuotationResponse(
						SupplierId.of(entry.getKey()),
						entry.getValue().stream()
								.map(line -> new QuotationItemPrice(line.getProductId(), line.getUnitPrice()))
								.toList(),
						entry.getValue().get(0).getDeadline()))
				.toList();
	}

	private List<QuotationResponseLineEmbeddable> toResponseLineEmbeddables(final List<QuotationResponse> responses) {
		return responses.stream()
				.flatMap(response -> response.itemPrices().stream()
						.map(itemPrice -> QuotationResponseLineEmbeddable.builder()
								.supplierId(response.supplierId().value())
								.deadline(response.deadline())
								.productId(itemPrice.productId())
								.unitPrice(itemPrice.unitPrice())
								.build()))
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
