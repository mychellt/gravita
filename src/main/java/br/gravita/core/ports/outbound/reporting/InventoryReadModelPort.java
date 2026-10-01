package br.gravita.core.ports.outbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryReadModelPort {

	/** What the given sold quantities cost at each product's current average cost. */
	BigDecimal costOfGoodsSold(List<SoldQuantity> sold);

	/**
	 * The products whose available stock (summed over warehouses) is below their minimum, and the lots with stock
	 * left that expire on or before {@code today + nearExpiryDays} (expired ones included).
	 */
	List<StockAlert> criticalStock(LocalDate today, int nearExpiryDays, UUID companyId);

	/**
	 * For every product that has stock or moved in {@code [from, to]}: what was issued in it, and the quantity on
	 * hand (summed over warehouses) at the start of {@code from} and at the end of {@code to}.
	 */
	List<StockFlow> stockFlows(LocalDate from, LocalDate to, UUID companyId);

	record SoldQuantity(UUID productId, BigDecimal quantity) {
	}

	record StockFlow(UUID productId, BigDecimal issued, BigDecimal openingOnHand, BigDecimal closingOnHand) {
	}

	/** Exactly one of the below-minimum fields ({@code available}, {@code minimum}) or lot fields is set. */
	record StockAlert(UUID productId, BigDecimal available, BigDecimal minimum, UUID warehouseId, String lotCode,
			LocalDate expiryDate) {

		public boolean isBelowMinimum() {
			return minimum != null;
		}
	}
}
