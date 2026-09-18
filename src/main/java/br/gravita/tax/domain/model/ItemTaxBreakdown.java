package br.gravita.tax.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ItemTaxBreakdown(int itemIndex, String productRef, List<TaxLineBreakdown> taxLines) {

	public ItemTaxBreakdown {
		Objects.requireNonNull(productRef, "productRef");
		taxLines = taxLines == null ? List.of() : List.copyOf(taxLines);
	}

	public BigDecimal totalAmount() {
		return taxLines.stream().map(TaxLineBreakdown::finalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
