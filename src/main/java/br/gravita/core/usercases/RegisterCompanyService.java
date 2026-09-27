package br.gravita.core.usercases;

import br.gravita.core.ports.inbound.masterdata.RegisterCompanyCommand;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.annotations.UseCase;

import java.util.UUID;

@UseCase
public class RegisterCompanyService implements RegisterCompanyUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;
	private final DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	public RegisterCompanyService(CompanyRepositoryPort companyRepositoryPort,
			DocumentSeriesRepositoryPort documentSeriesRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
	}

	@Override
	public CompanyId execute(RegisterCompanyCommand command) {
		validateParentCompany(command.parentCompanyId());

		boolean isNewCompany = command.id() == null;
		Company company = isNewCompany ? registerNewCompany(command) : updateExistingCompany(command);
		Company saved = companyRepositoryPort.save(company);

		if (isNewCompany) {
			createInitialDocumentSeries(saved.getId());
		}
		return saved.getId();
	}

	private Company registerNewCompany(RegisterCompanyCommand command) {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		return Company.of(id, command.cnpj(), command.ie(), command.im(), command.cnae(), command.taxRegime(),
				command.simplesOptante(), SefazEnvironment.HOMOLOGATION, command.address(), command.state(),
				command.issuingEmail(), command.phone(), command.logoUrl(), command.parentCompanyId());
	}

	private Company updateExistingCompany(RegisterCompanyCommand command) {
		Company existing = companyRepositoryPort.findById(command.id())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + command.id().value()));
		return Company.of(existing.getId(), command.cnpj(), command.ie(), command.im(), command.cnae(),
				command.taxRegime(), command.simplesOptante(), existing.getSefazEnvironment(), command.address(),
				command.state(), command.issuingEmail(), command.phone(), command.logoUrl(),
				command.parentCompanyId());
	}

	private void validateParentCompany(CompanyId parentCompanyId) {
		if (parentCompanyId != null && companyRepositoryPort.findById(parentCompanyId).isEmpty()) {
			throw new BusinessRuleException("Parent company not found: " + parentCompanyId.value());
		}
	}

	private void createInitialDocumentSeries(CompanyId companyId) {
		for (FiscalDocumentType documentType : FiscalDocumentType.values()) {
			documentSeriesRepositoryPort.save(DocumentSeries.placeholder(companyId, documentType));
		}
	}
}
