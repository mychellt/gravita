package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import java.util.UUID;

public record RegisterMunicipalityIntegrationResponse(UUID id) {

	public static RegisterMunicipalityIntegrationResponse from(final MunicipalityIntegrationId id) {
		return new RegisterMunicipalityIntegrationResponse(id.value());
	}
}
