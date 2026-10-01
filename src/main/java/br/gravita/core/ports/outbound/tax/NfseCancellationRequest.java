package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.NfseDocument;
import java.util.Objects;

public record NfseCancellationRequest(NfseDocument document, MunicipalityIntegration integration,
		String justification) {

	public NfseCancellationRequest {
		Objects.requireNonNull(document, "document");
		Objects.requireNonNull(integration, "integration");
		Objects.requireNonNull(justification, "justification");
	}
}
