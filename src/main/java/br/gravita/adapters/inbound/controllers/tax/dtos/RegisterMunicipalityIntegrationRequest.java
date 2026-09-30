package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.ports.inbound.tax.RegisterMunicipalityIntegrationCommand;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RegisterMunicipalityIntegrationRequest(@NotNull NfseStandard standard, String version,
		String webserviceUrl, @NotNull CertificateType requiredCertificateType, List<String> requiredFields,
		Boolean homologated) {

	public RegisterMunicipalityIntegrationCommand toCommand(String ibgeCode) {
		return new RegisterMunicipalityIntegrationCommand(ibgeCode, standard, version, webserviceUrl,
				requiredCertificateType, requiredFields, Boolean.TRUE.equals(homologated));
	}
}
