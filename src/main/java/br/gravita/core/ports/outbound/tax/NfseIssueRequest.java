package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.NfseDocument;
import java.util.Objects;

public record NfseIssueRequest(NfseDocument document, MunicipalityIntegration integration) {

	public NfseIssueRequest {
		Objects.requireNonNull(document, "document");
		Objects.requireNonNull(integration, "integration");
	}
}
