package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.MunicipalityIntegrationId;

public interface RegisterMunicipalityIntegrationUseCase {

	MunicipalityIntegrationId execute(RegisterMunicipalityIntegrationCommand command);
}
