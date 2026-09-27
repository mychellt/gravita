package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.Objects;

public record TaxOverrideInput(int itemIndex, TaxType taxType, BigDecimal value, String justification) {

	public TaxOverrideInput {
		Objects.requireNonNull(taxType, "taxType");
		Objects.requireNonNull(value, "value");
		if (justification == null || justification.isBlank()) {
			throw new TaxDomainException(
					"Manual tax override for item " + itemIndex + " (" + taxType + ") requires a non-empty justification.");
		}
	}
}
