package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;

public interface RegisterCompanyUseCase {
	CompanyId execute(RegisterCompanyCommand command);
}
