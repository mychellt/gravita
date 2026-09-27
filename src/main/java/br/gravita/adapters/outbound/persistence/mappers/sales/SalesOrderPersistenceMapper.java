package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderJpaEntity;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalesOrderPersistenceMapper {

	default SalesOrder toDomain(final SalesOrderJpaEntity entity) {
		return SalesOrder.of(
				SalesOrderId.of(entity.getId()),
				QuoteId.of(entity.getOriginQuoteId()),
				entity.getCustomerId(),
				entity.getSalespersonId(),
				toItems(entity.getItems()),
				entity.getStatus(),
				entity.getApprovedBy(),
				entity.getAlcadaId(),
				entity.getCancelReason(),
				entity.getInvoicedAt());
	}

	default SalesOrderJpaEntity toEntity(final SalesOrder domain) {
		return SalesOrderJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.originQuoteId(domain.getOriginQuoteId() == null ? null : domain.getOriginQuoteId().value())
				.customerId(domain.getCustomerId())
				.salespersonId(domain.getSalespersonId())
				.status(domain.getStatus())
				.approvedBy(domain.getApprovedBy())
				.alcadaId(domain.getAlcadaId())
				.items(toItemEmbeddables(domain.getItems()))
				.cancelReason(domain.getCancelReason())
				.invoicedAt(domain.getInvoicedAt())
				.build();
	}

	private List<SalesOrderItem> toItems(final List<SalesOrderItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new SalesOrderItem(e.getProductOrServiceId(), e.getQuantity(), e.getUnitPrice(),
						e.getDiscount()))
				.toList();
	}

	private List<SalesOrderItemEmbeddable> toItemEmbeddables(final List<SalesOrderItem> items) {
		return items.stream()
				.map(item -> SalesOrderItemEmbeddable.builder()
						.productOrServiceId(item.productOrServiceId())
						.quantity(item.quantity())
						.unitPrice(item.unitPrice())
						.discount(item.discount())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
