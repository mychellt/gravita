package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import java.util.Objects;

/**
 * UC-M2-05: submits a CC-e (Carta de Correção Eletrônica) event for an
 * already-{@code AUTHORIZED} NFe.
 */
public record SefazCorrectionRequest(CompanyId companyId, SefazEnvironment environment, String accessKey,
		int sequenceNumber, String text) {

	public SefazCorrectionRequest {
		Objects.requireNonNull(companyId, "companyId");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(accessKey, "accessKey");
		if (sequenceNumber < 1) {
			throw new IllegalArgumentException("sequenceNumber must be positive: " + sequenceNumber);
		}
		Objects.requireNonNull(text, "text");
	}
}
