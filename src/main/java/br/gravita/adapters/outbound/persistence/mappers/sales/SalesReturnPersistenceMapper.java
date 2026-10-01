package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesReturnItemEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.SalesReturnJpaEntity;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnItem;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalesReturnPersistenceMapper {

	@Mapping(target = "id.value", source = "id")
	@Mapping(target = "orderId.value", source = "salesOrderId")
	@Mapping(target = "returnNfeRef", source = "entity", qualifiedByName = "toReturnNfeRef",
			conditionExpression = "java(entity.getReturnNfeDocumentType() != null && entity.getReturnNfeDocumentId() != null)")
	SalesReturn map(final SalesReturnJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "salesOrderId", source = "orderId.value")
	@Mapping(target = "returnNfeDocumentType", source = "returnNfeRef.type")
	@Mapping(target = "returnNfeDocumentId", source = "returnNfeRef.documentId")
	SalesReturnJpaEntity map(final SalesReturn domain);

	SalesReturnItem map(final SalesReturnItemEmbeddable embeddable);

	SalesReturnItemEmbeddable map(final SalesReturnItem item);

	@Named("toReturnNfeRef")
	@Mapping(target = "type", source = "returnNfeDocumentType")
	@Mapping(target = "documentId", source = "returnNfeDocumentId")
	FiscalDocumentRef toReturnNfeRef(final SalesReturnJpaEntity entity);
}
