package br.gravita.tax.domain.model;

import java.math.BigDecimal;

/**
 * The computed result for a single tax on a single item. {@code computedAmount}
 * is always the engine's own calculation, kept for audit even when
 * {@code overridden} replaces {@code finalAmount} with a manual value.
 */
public record TaxLineBreakdown(
		TaxType taxType,
		BigDecimal base,
		BigDecimal ratePercentage,
		BigDecimal computedAmount,
		BigDecimal finalAmount,
		boolean overridden,
		String overrideJustification) {
}
