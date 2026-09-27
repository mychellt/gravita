package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceStatus;
import java.util.List;
import java.util.UUID;

public record SalesInvoiceView(UUID id, UUID orderId, List<FiscalDocumentRef> fiscalDocuments,
		SalesInvoiceStatus status) {

	public static SalesInvoiceView from(SalesInvoice invoice) {
		return new SalesInvoiceView(invoice.getId().value(), invoice.getOrderId().value(),
				invoice.getFiscalDocuments(), invoice.getStatus());
	}
}
