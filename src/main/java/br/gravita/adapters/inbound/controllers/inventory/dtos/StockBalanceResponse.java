package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.StockBalanceView;

import java.math.BigDecimal;
import java.util.UUID;

public record StockBalanceResponse(UUID productId, UUID warehouseId, BigDecimal onHand, BigDecimal reserved,
		BigDecimal inTransit, BigDecimal available, BigDecimal averageCost) {

	public static StockBalanceResponse from(StockBalanceView view) {
		return new StockBalanceResponse(view.productId(), view.warehouseId(), view.onHand(), view.reserved(),
				view.inTransit(), view.available(), view.averageCost());
	}
}
