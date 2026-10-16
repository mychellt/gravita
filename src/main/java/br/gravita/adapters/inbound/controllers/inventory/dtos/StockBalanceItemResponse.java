package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.StockBalanceItemView;
import java.math.BigDecimal;
import java.util.UUID;

public record StockBalanceItemResponse(UUID productId, String productCode, UUID warehouseId, BigDecimal onHand,
		BigDecimal reserved, BigDecimal inTransit, BigDecimal available, BigDecimal averageCost,
		boolean allowNegativeStock) {

	public static StockBalanceItemResponse from(final StockBalanceItemView view) {
		return new StockBalanceItemResponse(view.productId(), view.productCode(), view.warehouseId(), view.onHand(),
				view.reserved(), view.inTransit(), view.available(), view.averageCost(), view.allowNegativeStock());
	}
}
