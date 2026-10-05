package br.gravita.core.domain;

import java.math.BigDecimal;

public record StockParametersDomain(BigDecimal minimum, BigDecimal maximum, BigDecimal reorderPoint) {

	public boolean isAtOrBelowReorderPoint(final BigDecimal available) {
		return reorderPoint != null && available.compareTo(reorderPoint) <= 0;
	}

	public BigDecimal replenishmentQuantity(final BigDecimal available) {
		if (maximum == null) {
			return BigDecimal.ZERO;
		}
		final BigDecimal quantity = maximum.subtract(available);
		return quantity.max(BigDecimal.ZERO);
	}
}
