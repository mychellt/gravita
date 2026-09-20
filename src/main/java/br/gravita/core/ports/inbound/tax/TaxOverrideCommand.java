package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.TaxType;

import java.math.BigDecimal;

public record TaxOverrideCommand(int itemIndex, TaxType tax, BigDecimal value, String justification) {
}
