package br.gravita.core.domain;

import java.math.BigDecimal;

public record StockParametersDomain(BigDecimal minimum, BigDecimal maximum, BigDecimal reorderPoint) {

	public boolean isAtOrBelowReorderPoint(BigDecimal available) {
		return reorderPoint != null && available.compareTo(reorderPoint) <= 0;
	}

	public BigDecimal replenishmentQuantity(BigDecimal available) {
		if (maximum == null) {
			return BigDecimal.ZERO;
		}
		BigDecimal quantity = maximum.subtract(available);
		return quantity.max(BigDecimal.ZERO);
	}
}
