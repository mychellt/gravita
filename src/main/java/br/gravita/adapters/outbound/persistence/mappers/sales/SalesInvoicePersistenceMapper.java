package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FiscalDocumentRefEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.SalesInvoiceJpaEntity;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesInvoice;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalesInvoicePersistenceMapper {

	@Mapping(target = "id.value", source = "id")
	@Mapping(target = "orderId.value", source = "salesOrderId")
	SalesInvoice map(final SalesInvoiceJpaEntity entity);

	@Mapping(target = "id", source = "id.value")
	@Mapping(target = "salesOrderId", source = "orderId.value")
	SalesInvoiceJpaEntity map(final SalesInvoice domain);

	@Mapping(target = "type", source = "documentType")
	FiscalDocumentRef map(final FiscalDocumentRefEmbeddable embeddable);

	@Mapping(target = "documentType", source = "type")
	FiscalDocumentRefEmbeddable map(final FiscalDocumentRef ref);
}
