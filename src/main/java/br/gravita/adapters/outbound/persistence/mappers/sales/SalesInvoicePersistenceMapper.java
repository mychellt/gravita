package br.gravita.adapters.outbound.persistence.mappers.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.FiscalDocumentRefEmbeddable;
import br.gravita.adapters.outbound.persistence.entities.sales.SalesInvoiceJpaEntity;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import br.gravita.core.domain.sales.SalesOrderId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(builder = @Builder(disableBuilder = true), nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface SalesInvoicePersistenceMapper {

	default SalesInvoice toDomain(final SalesInvoiceJpaEntity entity) {
		return SalesInvoice.of(SalesInvoiceId.of(entity.getId()), SalesOrderId.of(entity.getSalesOrderId()),
				toFiscalDocuments(entity.getFiscalDocuments()), entity.getStatus());
	}

	default SalesInvoiceJpaEntity toEntity(final SalesInvoice domain) {
		return SalesInvoiceJpaEntity.builder()
				.id(domain.getId() == null ? null : domain.getId().value())
				.salesOrderId(domain.getOrderId() == null ? null : domain.getOrderId().value())
				.status(domain.getStatus())
				.fiscalDocuments(toFiscalDocumentEmbeddables(domain.getFiscalDocuments()))
				.build();
	}

	private List<FiscalDocumentRef> toFiscalDocuments(final List<FiscalDocumentRefEmbeddable> embeddables) {
		if (embeddables == null) {
			return List.of();
		}
		return embeddables.stream()
				.map(e -> new FiscalDocumentRef(e.getDocumentType(), e.getDocumentId()))
				.toList();
	}

	private List<FiscalDocumentRefEmbeddable> toFiscalDocumentEmbeddables(final List<FiscalDocumentRef> refs) {
		return refs.stream()
				.map(ref -> FiscalDocumentRefEmbeddable.builder()
						.documentType(ref.type())
						.documentId(ref.documentId())
						.build())
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
