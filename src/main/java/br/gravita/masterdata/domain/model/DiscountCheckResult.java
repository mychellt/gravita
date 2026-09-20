package br.gravita.masterdata.domain.model;

/**
 * Outcome of {@link PriceTable#evaluateDiscount(java.math.BigDecimal)}. A
 * {@code BLOCK} table never reaches here — it throws instead — so callers
 * (the future `sales` module) only see {@code ALLOWED} or {@code ALERT}.
 */
public enum DiscountCheckResult {
	ALLOWED,
	ALERT
}
