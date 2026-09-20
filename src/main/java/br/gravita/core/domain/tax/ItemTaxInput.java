package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * One item line already resolved to the rate rows that apply to it, ready
 * for {@code TaxEngine} to compute.
 */
public record ItemTaxInput(
		int itemIndex,
		String productRef,
		BigDecimal quantity,
		BigDecimal unitPrice,
		List<TaxRateRule> applicableRates) {

	public ItemTaxInput {
		Objects.requireNonNull(productRef, "productRef");
		Objects.requireNonNull(quantity, "quantity");
		Objects.requireNonNull(unitPrice, "unitPrice");
		applicableRates = applicableRates == null ? List.of() : List.copyOf(applicableRates);
	}

	public BigDecimal grossAmount() {
		return quantity.multiply(unitPrice);
	}
}
