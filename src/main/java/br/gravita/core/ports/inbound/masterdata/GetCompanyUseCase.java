package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;

public interface GetCompanyUseCase {
	/** @throws br.gravita.core.domain.masterdata.CompanyNotFoundException when there is no such company */
	Company execute(CompanyId id);
}
