package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import java.time.Instant;
import java.util.List;

public interface VoidedNumberRangeRepositoryPort {

	/**
	 * There is deliberately no update or delete counterpart - a
	 * {@link VoidedNumberRange} is immutable once created (UC-M2-06's AC2).
	 */
	VoidedNumberRange save(VoidedNumberRange voidedNumberRange);

	/**
	 * Ready for UC-M2-13 (Generate Livros Fiscais) to pick up the voided
	 * ranges for a period. {@code series}, {@code dateFrom} and
	 * {@code dateTo} are optional filters: a null {@code series} matches any
	 * series of the company, and a null bound leaves that side of the date
	 * range unbounded.
	 */
	List<VoidedNumberRange> findByCompanyIdAndSeriesAndDateRange(CompanyId companyId, String series, Instant dateFrom,
			Instant dateTo);
}
