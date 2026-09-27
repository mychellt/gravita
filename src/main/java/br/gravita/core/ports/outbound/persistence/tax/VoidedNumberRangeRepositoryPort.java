package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import java.util.List;

/**
 * Dedicated persistence for {@link VoidedNumberRange}: deliberately exposes
 * no update or delete operation, since the immutable audit record it stores
 * must never be revised once SEFAZ has accepted the void.
 */
public interface VoidedNumberRangeRepositoryPort {

	VoidedNumberRange save(VoidedNumberRange voidedNumberRange);

	List<VoidedNumberRange> findByCompanyId(CompanyId companyId);
}
