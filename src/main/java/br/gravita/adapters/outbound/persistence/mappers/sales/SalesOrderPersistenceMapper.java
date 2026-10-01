package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderJpaEntity;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderItem;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalesOrderPersistenceMapper {

	@Mapping(target = "id.value", source = "id")
	@Mapping(target = "originQuoteId.value", source = "originQuoteId")
	SalesOrder map(final SalesOrderJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "originQuoteId", source = "originQuoteId.value")
	SalesOrderJpaEntity map(final SalesOrder domain);

	SalesOrderItem map(final SalesOrderItemEmbeddable embeddable);

	SalesOrderItemEmbeddable map(final SalesOrderItem item);
}
