package br.gravita.tax.application.port.in;

import br.gravita.tax.domain.model.ItemTaxBreakdown;
import br.gravita.tax.domain.model.TaxCalculationTotals;

import java.util.List;

public record TaxCalculationResult(List<ItemTaxBreakdown> items, TaxCalculationTotals totals) {

	public TaxCalculationResult {
		items = items == null ? List.of() : List.copyOf(items);
	}
}
