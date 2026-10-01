package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;

/** Full postal address of an NFSe tomador, required when any tax is withheld at source (doc §5.2). */
public record TomadorAddress(String street, String number, String complement, String neighborhood,
		String zipCode, String state) {

	public TomadorAddress {
		requireNonBlank(street, "street");
		requireNonBlank(number, "number");
		requireNonBlank(neighborhood, "neighborhood");
		requireNonBlank(zipCode, "zipCode");
		requireNonBlank(state, "state");
	}

	private static void requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Tomador address " + field + " is required");
		}
	}
}
