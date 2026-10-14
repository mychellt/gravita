package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.TransportModalityDomain;

import java.util.UUID;

public record TransportModalityResponse(UUID id, String name, String code, boolean active) {

	public static TransportModalityResponse from(final TransportModalityDomain domain) {
		return new TransportModalityResponse(domain.getId(), domain.getName(), domain.getCode(), domain.isEnabled());
	}
}
