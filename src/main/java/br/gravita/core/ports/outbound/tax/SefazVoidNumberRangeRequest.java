package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import java.util.Objects;

public record SefazVoidNumberRangeRequest(CompanyId companyId, SefazEnvironment environment, String series,
		Long startNumber, Long endNumber, String justification) {

	public SefazVoidNumberRangeRequest {
		Objects.requireNonNull(companyId, "companyId");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(series, "series");
		Objects.requireNonNull(startNumber, "startNumber");
		Objects.requireNonNull(endNumber, "endNumber");
	}
}
