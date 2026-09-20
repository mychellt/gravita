package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.TaxCalculationTotals;

import java.util.List;

public record TaxCalculationResult(List<ItemTaxBreakdown> items, TaxCalculationTotals totals) {

	public TaxCalculationResult {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
