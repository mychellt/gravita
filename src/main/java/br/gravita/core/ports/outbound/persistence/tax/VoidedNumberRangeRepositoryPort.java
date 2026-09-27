package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import java.util.List;

public interface VoidedNumberRangeRepositoryPort {

	VoidedNumberRange save(VoidedNumberRange voidedNumberRange);

	List<VoidedNumberRange> findByCompanyId(CompanyId companyId);
}
