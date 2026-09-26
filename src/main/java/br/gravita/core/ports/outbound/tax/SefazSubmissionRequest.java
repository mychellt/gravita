package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import java.math.BigDecimal;
import java.util.Objects;

public record SefazSubmissionRequest(CompanyId companyId, SefazEnvironment environment, String accessKey,
		BigDecimal saleTotal, TaxCalculationResult taxResult) {

	public SefazSubmissionRequest {
		Objects.requireNonNull(companyId, "companyId");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(accessKey, "accessKey");
		Objects.requireNonNull(saleTotal, "saleTotal");
		Objects.requireNonNull(taxResult, "taxResult");
	}
}
