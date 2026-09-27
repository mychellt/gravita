package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import java.time.Instant;
import java.util.List;

public interface VoidedNumberRangeRepositoryPort {

	VoidedNumberRange save(VoidedNumberRange voidedNumberRange);

	List<VoidedNumberRange> findByCompanyId(CompanyId companyId);

	/**
	 * Query shape UC-M2-13 (Livros Fiscais, Phase 7) will use to pull the
	 * voided ranges for a given series and reporting period.
	 */
	List<VoidedNumberRange> findByCompanyIdAndSeriesAndVoidedAtBetween(CompanyId companyId, String series,
			Instant voidedFrom, Instant voidedTo);
}
