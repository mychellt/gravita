package br.gravita.core.domain;

public record PersonLookupResult(String name, AddressDomain address) {

	public static PersonLookupResult notFound() {
		return new PersonLookupResult(null, null);
	}
}
