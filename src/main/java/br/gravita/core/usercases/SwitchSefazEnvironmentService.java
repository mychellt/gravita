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

	public SwitchSefazEnvironmentService(final CompanyRepositoryPort companyRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
	}

	@Override
	public void execute(final SwitchSefazEnvironmentCommand command) {
		final Company existing = companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new CompanyNotFoundException(command.companyId().value()));

		final Company updated = Company.builder()
				.id(existing.getId())
				.name(existing.getName())
				.cnpj(existing.getCnpj())
				.ie(existing.getIe())
				.im(existing.getIm())
				.cnae(existing.getCnae())
				.taxRegime(existing.getTaxRegime())
				.simplesOptante(existing.isSimplesOptante())
				.sefazEnvironment(command.environment())
				.address(existing.getAddress())
				.state(existing.getState())
				.issuingEmail(existing.getIssuingEmail())
				.phone(existing.getPhone())
				.logoUrl(existing.getLogoUrl())
				.parentCompanyId(existing.getParentCompanyId())
				.build();

		companyRepositoryPort.save(updated);
	}
}
