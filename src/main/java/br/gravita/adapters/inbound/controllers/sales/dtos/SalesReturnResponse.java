package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesReturnItem;
import br.gravita.core.ports.inbound.sales.SalesReturnView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SalesReturnResponse(UUID id, UUID orderId, List<ItemResponse> items, FiscalDocumentResponse returnNfeRef,
		boolean total) {

	public static SalesReturnResponse from(SalesReturnView view) {
		return new SalesReturnResponse(view.id(), view.orderId(),
				view.items().stream().map(ItemResponse::from).toList(),
				FiscalDocumentResponse.from(view.returnNfeRef()), view.total());
	}

	public record ItemResponse(UUID productOrServiceId, BigDecimal quantity) {

		static ItemResponse from(SalesReturnItem item) {
			return new ItemResponse(item.productOrServiceId(), item.quantity());
		}
	}

	public record FiscalDocumentResponse(FiscalDocumentType type, UUID documentId) {

		static FiscalDocumentResponse from(FiscalDocumentRef ref) {
			return ref == null ? null : new FiscalDocumentResponse(ref.type(), ref.documentId());
		}
	}
}
