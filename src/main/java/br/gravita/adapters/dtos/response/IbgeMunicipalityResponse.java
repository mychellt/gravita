package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.IbgeMunicipalityDomain;

import java.util.UUID;

public record IbgeMunicipalityResponse(UUID id, String ibgeCode, String name, String stateCode) {

	public static IbgeMunicipalityResponse from(final IbgeMunicipalityDomain domain) {
		return new IbgeMunicipalityResponse(domain.getId(), domain.getIbgeCode(), domain.getName(), domain.getStateCode());
	}
}
