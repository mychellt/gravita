package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.QuoteItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.QuoteJpaEntity;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface QuotePersistenceMapper {

	default Quote toDomain(final QuoteJpaEntity entity) {
		return Quote.of(
				QuoteId.of(entity.getId()),
				entity.getCustomerId(),
				toItems(entity.getItems()),
				entity.getValidUntil(),
				entity.getStatus());
	}

	default QuoteJpaEntity toEntity(final Quote domain) {
		return QuoteJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.customerId(domain.getCustomerId())
				.validUntil(domain.getValidUntil())
				.status(domain.getStatus())
				.items(toItemEmbeddables(domain.getItems()))
				.build();
	}

	private List<QuoteItem> toItems(final List<QuoteItemEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new QuoteItem(e.getProductOrServiceId(), e.getQuantity(), e.getUnitPrice(), e.getDiscount()))
				.toList();
	}

	private List<QuoteItemEmbeddable> toItemEmbeddables(final List<QuoteItem> items) {
		return items.stream()
				.map(item -> QuoteItemEmbeddable.builder()
						.productOrServiceId(item.productOrServiceId())
						.quantity(item.quantity())
						.unitPrice(item.unitPrice())
						.discount(item.discount())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
