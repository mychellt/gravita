package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.core.domain.tax.TaxRegime;

import java.util.Objects;

public record TaxRateQuery(
		String ncm,
		String originState,
		String destinationState,
		TaxRegime regime,
		String operationType) {

	public TaxRateQuery {
		Objects.requireNonNull(ncm, "ncm");
		Objects.requireNonNull(originState, "originState");
		Objects.requireNonNull(destinationState, "destinationState");
		Objects.requireNonNull(regime, "regime");
		Objects.requireNonNull(operationType, "operationType");
	}
}
