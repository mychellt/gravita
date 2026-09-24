package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.StockParametersDomain;
import br.gravita.core.domain.inventory.StockBalance;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result of {@link SuggestReorderUseCase}: a product/warehouse pair whose
 * {@code available} balance is at or below its configured reorder point,
 * with the quantity needed to bring it back up to the configured maximum
 * (UC-M5-11, AC1/AC2).
 */
public record ReorderSuggestion(UUID productId, UUID warehouseId, BigDecimal available, BigDecimal reorderPoint,
		BigDecimal suggestedQuantity) {

	public static ReorderSuggestion of(StockBalance balance, StockParametersDomain stock) {
		BigDecimal available = balance.available();
		return new ReorderSuggestion(balance.getProductId(), balance.getWarehouseId(), available, stock.reorderPoint(),
				stock.replenishmentQuantity(available));
	}
}
