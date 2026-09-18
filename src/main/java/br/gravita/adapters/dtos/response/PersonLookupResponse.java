package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PersonLookupResult;

public record PersonLookupResponse(String name, AddressResponse address) {

	public static PersonLookupResponse from(PersonLookupResult result) {
		return new PersonLookupResponse(result.name(), AddressResponse.from(result.address()));
	}
}
