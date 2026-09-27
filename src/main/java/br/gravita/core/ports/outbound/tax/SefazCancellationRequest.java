package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import java.util.Objects;

public record SefazCancellationRequest(CompanyId companyId, SefazEnvironment environment, String accessKey,
		String protocol, String reason) {

	public SefazCancellationRequest {
		Objects.requireNonNull(companyId, "companyId");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(accessKey, "accessKey");
		Objects.requireNonNull(protocol, "protocol");
	}
}
