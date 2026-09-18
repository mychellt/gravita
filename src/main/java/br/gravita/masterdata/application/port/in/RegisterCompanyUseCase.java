package br.gravita.masterdata.application.port.in;

import br.gravita.masterdata.domain.model.CompanyId;

public interface RegisterCompanyUseCase {
	CompanyId execute(RegisterCompanyCommand command);
}
