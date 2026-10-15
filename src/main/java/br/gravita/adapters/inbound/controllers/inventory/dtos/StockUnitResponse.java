package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.ListLotsQuery;
import br.gravita.core.ports.inbound.inventory.StockUnitView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A lot ({@code expiryDate} and {@code quantity} set, no {@code status}) or a serial unit ({@code status} set). */
public record StockUnitResponse(ListLotsQuery.Kind kind, UUID id, UUID productId, String productCode,
		UUID warehouseId, String code, LocalDate expiryDate, BigDecimal quantity, String status) {

	public static StockUnitResponse from(final StockUnitView view) {
		return new StockUnitResponse(view.kind(), view.id(), view.productId(), view.productCode(),
				view.warehouseId(), view.code(), view.expiryDate(), view.quantity(), view.status());
	}
}
