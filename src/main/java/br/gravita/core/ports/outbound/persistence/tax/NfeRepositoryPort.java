package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NfeRepositoryPort {

	NfeDocument save(NfeDocument document);

	Optional<NfeDocument> findById(NfeDocumentId id);

	/** The NFe authorized over {@code [from, to)}, oldest authorization first. */
	List<NfeDocument> findAuthorizedBetween(Instant from, Instant to);

	/** The NFe {@code companyId} issued and that were authorized over {@code [from, to)}, oldest authorization first. */
	List<NfeDocument> findAuthorizedByCompanyBetween(CompanyId companyId, Instant from, Instant to);

	/**
	 * UC-M2-11 (SPED Fiscal): the NFe {@code companyId} issued that SEFAZ authorized over {@code [from, to)} and that
	 * are still authorized or have since been cancelled - a cancelled NFe stays in the file of the month it was
	 * issued in - oldest authorization first.
	 */
	List<NfeDocument> findAuthorizedOrCancelledByCompanyBetween(CompanyId companyId, Instant from, Instant to);
}
