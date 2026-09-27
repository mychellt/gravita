package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesInvoiceStatus;
import br.gravita.core.ports.inbound.sales.SalesInvoiceView;
import java.util.List;
import java.util.UUID;

public record SalesInvoiceResponse(UUID id, UUID orderId, List<FiscalDocumentResponse> fiscalDocuments,
		SalesInvoiceStatus status) {

	public static SalesInvoiceResponse from(SalesInvoiceView view) {
		return new SalesInvoiceResponse(view.id(), view.orderId(),
				view.fiscalDocuments().stream().map(FiscalDocumentResponse::from).toList(), view.status());
	}

	public record FiscalDocumentResponse(FiscalDocumentType type, UUID documentId) {

		static FiscalDocumentResponse from(FiscalDocumentRef ref) {
			return new FiscalDocumentResponse(ref.type(), ref.documentId());
		}
	}
}
