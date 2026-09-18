package br.gravita.masterdata.application.service;

import br.gravita.masterdata.application.port.in.RegisterCompanyCommand;
import br.gravita.masterdata.application.port.in.RegisterCompanyUseCase;
import br.gravita.masterdata.application.port.out.CompanyRepositoryPort;
import br.gravita.masterdata.application.port.out.DocumentSeriesRepositoryPort;
import br.gravita.masterdata.domain.model.Company;
import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.masterdata.domain.model.DocumentSeries;
import br.gravita.masterdata.domain.model.FiscalDocumentType;
import br.gravita.masterdata.domain.model.SefazEnvironment;
import br.gravita.shared.BusinessRuleException;
import br.gravita.shared.UseCase;

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
				command.simplesOptante(), SefazEnvironment.HOMOLOGATION, command.address(), command.issuingEmail(),
				command.phone(), command.logoUrl(), command.parentCompanyId());
	}

	private Company updateExistingCompany(RegisterCompanyCommand command) {
		Company existing = companyRepositoryPort.findById(command.id())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + command.id().value()));
		return Company.of(existing.getId(), command.cnpj(), command.ie(), command.im(), command.cnae(),
				command.taxRegime(), command.simplesOptante(), existing.getSefazEnvironment(), command.address(),
				command.issuingEmail(), command.phone(), command.logoUrl(), command.parentCompanyId());
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
