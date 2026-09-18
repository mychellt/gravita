package br.gravita.tax.application.port.in;

import br.gravita.tax.domain.model.TaxRegime;

import java.util.List;
import java.util.Objects;

public record CalculateTaxCommand(
		List<TaxItemCommand> items,
		String originState,
		String destinationState,
		TaxRegime taxRegime,
		String operationType,
		List<TaxOverrideCommand> overrides) {

	public CalculateTaxCommand {
		Objects.requireNonNull(items, "items");
		Objects.requireNonNull(originState, "originState");
		Objects.requireNonNull(destinationState, "destinationState");
		Objects.requireNonNull(taxRegime, "taxRegime");
		Objects.requireNonNull(operationType, "operationType");
		items = List.copyOf(items);
		overrides = overrides == null ? List.of() : List.copyOf(overrides);
	}
}
