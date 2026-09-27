package br.gravita.core.domain.tax;

import java.math.BigDecimal;

public record TaxLineBreakdown(
		TaxType taxType,
		BigDecimal base,
		BigDecimal ratePercentage,
		BigDecimal computedAmount,
		BigDecimal finalAmount,
		boolean overridden,
		String overrideJustification) {
}
