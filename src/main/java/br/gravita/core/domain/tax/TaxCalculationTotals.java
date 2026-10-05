package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record TaxCalculationTotals(Map<TaxType, BigDecimal> byTaxType, BigDecimal grandTotal) {

	public static TaxCalculationTotals from(final List<ItemTaxBreakdown> items) {
		final Map<TaxType, BigDecimal> byType = new EnumMap<>(TaxType.class);
		for (final ItemTaxBreakdown item : items) {
			for (final TaxLineBreakdown line : item.taxLines()) {
				byType.merge(line.taxType(), line.finalAmount(), BigDecimal::add);
			}
		}
		final BigDecimal grandTotal = byType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
		return new TaxCalculationTotals(Map.copyOf(byType), grandTotal);
	}
}
