package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.tax.NfseStandard;
import java.util.List;
import java.util.Objects;

public record RegisterMunicipalityIntegrationCommand(String ibgeCode, NfseStandard standard, String version,
		String webserviceUrl, CertificateType requiredCertificateType, List<String> requiredFields,
		boolean homologated) {

	public RegisterMunicipalityIntegrationCommand {
		Objects.requireNonNull(ibgeCode, "ibgeCode is required");
		Objects.requireNonNull(standard, "standard is required");
		Objects.requireNonNull(requiredCertificateType, "requiredCertificateType is required");
	}
}
