package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;

/** AC4: a manual override of one item's computed tax line; requires a justification. */
public record NfeItemTaxOverrideInput(TaxType tax, BigDecimal value, String justification) {
}
