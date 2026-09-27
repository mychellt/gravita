package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentCommand;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;

@UseCase
public class SwitchSefazEnvironmentService implements SwitchSefazEnvironmentUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;

	public SwitchSefazEnvironmentService(CompanyRepositoryPort companyRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
	}

	@Override
	public void execute(SwitchSefazEnvironmentCommand command) {
		Company existing = companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new CompanyNotFoundException(command.companyId().value()));

		Company updated = Company.of(existing.getId(), existing.getCnpj(), existing.getIe(), existing.getIm(),
				existing.getCnae(), existing.getTaxRegime(), existing.isSimplesOptante(), command.environment(),
				existing.getAddress(), existing.getState(), existing.getIssuingEmail(), existing.getPhone(),
				existing.getLogoUrl(), existing.getParentCompanyId());

		companyRepositoryPort.save(updated);
	}
}
