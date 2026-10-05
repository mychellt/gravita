package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.ports.inbound.masterdata.GetCompanyUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;

@UseCase
public class GetCompanyService implements GetCompanyUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;

	public GetCompanyService(final CompanyRepositoryPort companyRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
	}

	@Override
	public Company execute(final CompanyId id) {
		return companyRepositoryPort.findById(id).orElseThrow(() -> new CompanyNotFoundException(id.value()));
	}
}
