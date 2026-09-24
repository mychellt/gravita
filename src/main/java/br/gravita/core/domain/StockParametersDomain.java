package br.gravita.core.domain;

import java.math.BigDecimal;

/**
 * Single source of truth for the "low stock" comparison (module spec §6.2):
 * both {@code SuggestReorderUseCase} (UC-M5-11) and the dashboard's min-stock
 * alert must evaluate {@code available <= reorderPoint} through this method
 * rather than duplicating the threshold check.
 */
public record StockParametersDomain(BigDecimal minimum, BigDecimal maximum, BigDecimal reorderPoint) {

	public boolean isAtOrBelowReorderPoint(BigDecimal available) {
		return reorderPoint != null && available.compareTo(reorderPoint) <= 0;
	}

	/**
	 * Quantity needed to bring {@code available} back up to the configured
	 * maximum (UC-M5-11, AC2) rather than just to the reorder point.
	 */
	public BigDecimal replenishmentQuantity(BigDecimal available) {
		if (maximum == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal quantity = maximum.subtract(available);
		return quantity.max(BigDecimal.ZERO);
	}
}
