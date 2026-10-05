package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import java.math.BigDecimal;
import java.util.Objects;

public record SefazSubmissionRequest(CompanyId companyId, SefazEnvironment environment, String accessKey,
		BigDecimal saleTotal, TaxCalculationResult taxResult, boolean contingency) {

	public SefazSubmissionRequest {
		Objects.requireNonNull(companyId, "companyId");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(accessKey, "accessKey");
		Objects.requireNonNull(saleTotal, "saleTotal");
		Objects.requireNonNull(taxResult, "taxResult");
	}

	/**
	 * NFC-e's online path never needs SVC-AN/SVC-RS contingency routing (its
	 * own contingency is a local queue-and-sync flow, not a SEFAZ endpoint
	 * switch) - this overload keeps its call site unchanged.
	 */
	public SefazSubmissionRequest(final CompanyId companyId, final SefazEnvironment environment, final String accessKey,
			final BigDecimal saleTotal, final TaxCalculationResult taxResult) {
		this(companyId, environment, accessKey, saleTotal, taxResult, false);
	}
}
