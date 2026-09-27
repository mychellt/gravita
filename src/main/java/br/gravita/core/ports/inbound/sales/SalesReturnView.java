package br.gravita.core.ports.inbound.sales;

import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnItem;
import java.util.List;
import java.util.UUID;

public record SalesReturnView(UUID id, UUID orderId, List<SalesReturnItem> items, FiscalDocumentRef returnNfeRef,
		boolean total) {

	public static SalesReturnView from(SalesReturn salesReturn) {
		return new SalesReturnView(salesReturn.getId().value(), salesReturn.getOrderId().value(),
				salesReturn.getItems(), salesReturn.getReturnNfeRef(), salesReturn.isTotal());
	}
}
