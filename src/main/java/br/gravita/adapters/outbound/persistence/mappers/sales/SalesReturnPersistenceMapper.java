package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesReturnItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.SalesReturnJpaEntity;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnId;
import br.gravita.core.domain.sales.SalesReturnItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalesReturnPersistenceMapper {

	default SalesReturn toDomain(final SalesReturnJpaEntity entity) {
		return SalesReturn.of(SalesReturnId.of(entity.getId()), SalesOrderId.of(entity.getSalesOrderId()),
				toItems(entity.getItems()), entity.isTotal(), toFiscalDocumentRef(entity));
	}

	default SalesReturnJpaEntity toEntity(final SalesReturn domain) {
		FiscalDocumentRef returnNfeRef = domain.getReturnNfeRef();
		return SalesReturnJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.salesOrderId(domain.getOrderId() == null ? null : domain.getOrderId().value())
				.total(domain.isTotal())
				.returnNfeDocumentType(returnNfeRef == null ? null : returnNfeRef.type())
				.returnNfeDocumentId(returnNfeRef == null ? null : returnNfeRef.documentId())
				.items(toItemEmbeddables(domain.getItems()))
				.build();
	}

	private FiscalDocumentRef toFiscalDocumentRef(final SalesReturnJpaEntity entity) {
		if (entity.getReturnNfeDocumentType() == null || entity.getReturnNfeDocumentId() == null) {
			return null;
		}
		return new FiscalDocumentRef(entity.getReturnNfeDocumentType(), entity.getReturnNfeDocumentId());
	}

	private List<SalesReturnItem> toItems(final List<SalesReturnItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new SalesReturnItem(e.getProductOrServiceId(), e.getQuantity()))
				.toList();
	}

	private List<SalesReturnItemEmbeddable> toItemEmbeddables(final List<SalesReturnItem> items) {
		return items.stream()
				.map(item -> SalesReturnItemEmbeddable.builder()
						.productOrServiceId(item.productOrServiceId())
						.quantity(item.quantity())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
