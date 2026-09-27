package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.util.Objects;

public record TaxRateRule(
		String ncm,
		String originState,
		String destinationState,
		TaxRegime regime,
		String operationType,
		TaxType taxType,
		BigDecimal ratePercentage,
		BigDecimal baseReductionPercentage,
		BigDecimal mvaPercentage) {

	public TaxRateRule {
		Objects.requireNonNull(ncm, "ncm");
		Objects.requireNonNull(originState, "originState");
		Objects.requireNonNull(destinationState, "destinationState");
		Objects.requireNonNull(regime, "regime");
		Objects.requireNonNull(operationType, "operationType");
		Objects.requireNonNull(taxType, "taxType");
		Objects.requireNonNull(ratePercentage, "ratePercentage");
		baseReductionPercentage = baseReductionPercentage == null ? BigDecimal.ZERO : baseReductionPercentage;
		mvaPercentage = mvaPercentage == null ? BigDecimal.ZERO : mvaPercentage;
	}
}
