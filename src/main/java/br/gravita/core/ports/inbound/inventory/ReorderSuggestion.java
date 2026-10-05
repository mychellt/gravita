package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;

import java.math.BigDecimal;
import java.util.UUID;

public record ReorderSuggestion(UUID productId, UUID warehouseId, BigDecimal available, BigDecimal reorderPoint,
		BigDecimal suggestedQuantity) {

	public static ReorderSuggestion of(final StockBalance balance, final StockParametersDomain stock) {
		final BigDecimal available = balance.available();
		return new ReorderSuggestion(balance.getProductId(), balance.getWarehouseId(), available, stock.reorderPoint(),
				stock.replenishmentQuantity(available));
	}
}
