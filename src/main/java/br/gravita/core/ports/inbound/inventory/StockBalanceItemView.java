package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.StockBalance;
import java.math.BigDecimal;
import java.util.UUID;

public record StockBalanceItemView(UUID productId, String productCode, UUID warehouseId, BigDecimal onHand,
		BigDecimal reserved, BigDecimal inTransit, BigDecimal available, BigDecimal averageCost) {

	public static StockBalanceItemView from(final StockBalance balance, final String productCode) {
		return new StockBalanceItemView(balance.getProductId(), productCode, balance.getWarehouseId(),
				balance.getOnHand(), balance.getReserved(), balance.getInTransit(), balance.available(),
				balance.getAverageCost());
	}
}
