package br.gravita.tax.application.port.in;

import br.gravita.tax.domain.model.TaxType;

import java.math.BigDecimal;

public record TaxOverrideCommand(int itemIndex, TaxType tax, BigDecimal value, String justification) {
}
