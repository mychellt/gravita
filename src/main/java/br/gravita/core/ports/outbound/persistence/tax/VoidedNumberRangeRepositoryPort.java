package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import java.time.Instant;
import java.util.List;

public interface VoidedNumberRangeRepositoryPort {

	VoidedNumberRange save(VoidedNumberRange voidedNumberRange);

	List<VoidedNumberRange> findByCompanyId(CompanyId companyId);

	/** The ranges voided for one series over {@code [voidedFrom, voidedTo]}. */
	List<VoidedNumberRange> findByCompanyIdAndSeriesAndVoidedAtBetween(CompanyId companyId, String series,
			Instant voidedFrom, Instant voidedTo);

	/**
	 * UC-M2-13 (Livros Fiscais): the ranges voided in every series over {@code [voidedFrom, voidedTo)}, oldest
	 * first, so the books of a period can explain the gaps in its numbering.
	 */
	List<VoidedNumberRange> findByCompanyIdAndVoidedAtBetween(CompanyId companyId, Instant voidedFrom,
			Instant voidedTo);
}
