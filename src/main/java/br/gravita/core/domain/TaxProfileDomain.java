package br.gravita.core.domain;

import java.math.BigDecimal;

public record TaxProfileDomain(
		BigDecimal icmsRate,
		BigDecimal ipiRate,
		BigDecimal pisRate,
		BigDecimal cofinsRate,
		BigDecimal icmsStRate,
		BigDecimal fcpRate) {
}
