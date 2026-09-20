package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A manual override of one computed tax line. Rejected here (not upstream)
 * so every caller — NFe, NFCe, NFSe, sales/purchasing preview — gets the
 * same "no justification, no override" rule for free.
 */
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
