package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.AddressDomain;

public record AddressResponse(
		String street,
		String number,
		String complement,
		String neighborhood,
		String city,
		String state,
		String zipCode) {

	public static AddressResponse from(AddressDomain domain) {
		if (domain == null) {
			return null;
		}
		return new AddressResponse(
				domain.getStreet(), domain.getNumber(), domain.getComplement(),
				domain.getNeighborhood(), domain.getCity(), domain.getState(), domain.getZipCode());
	}
}
