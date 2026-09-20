package br.gravita.masterdata.domain.model;

import br.gravita.shared.BusinessRuleException;

public record Address(String street, String number, String complement, String neighborhood, String city,
		String state, String zipCode) {

	public Address {
		requireNonBlank(street, "street");
		requireNonBlank(neighborhood, "neighborhood");
		requireNonBlank(city, "city");
		requireNonBlank(state, "state");
		requireNonBlank(zipCode, "zip code");
	}

	private static void requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Address " + field + " is required");
		}
	}
}
