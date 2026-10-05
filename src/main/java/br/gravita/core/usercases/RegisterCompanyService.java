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

	public RegisterCompanyService(final CompanyRepositoryPort companyRepositoryPort,
			final DocumentSeriesRepositoryPort documentSeriesRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
	}

	@Override
	public CompanyId execute(final RegisterCompanyCommand command) {
		validateParentCompany(command.parentCompanyId());

		final boolean isNewCompany = command.id() == null;
		final Company company = isNewCompany ? registerNewCompany(command) : updateExistingCompany(command);
		final Company saved = companyRepositoryPort.save(company);

		if (isNewCompany) {
			createInitialDocumentSeries(saved.getId());
		}
		return saved.getId();
	}

	private Company registerNewCompany(final RegisterCompanyCommand command) {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		return Company.builder()
				.id(id)
				.name(command.name())
				.cnpj(command.cnpj())
				.ie(command.ie())
				.im(command.im())
				.cnae(command.cnae())
				.taxRegime(command.taxRegime())
				.simplesOptante(command.simplesOptante())
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address(command.address())
				.state(command.state())
				.issuingEmail(command.issuingEmail())
				.phone(command.phone())
				.logoUrl(command.logoUrl())
				.parentCompanyId(command.parentCompanyId())
				.build();
	}

	private Company updateExistingCompany(final RegisterCompanyCommand command) {
		final Company existing = companyRepositoryPort.findById(command.id())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + command.id().value()));
		rejectCnpjChange(existing, command);
		return Company.builder()
				.id(existing.getId())
				.name(command.name())
				.cnpj(existing.getCnpj())
				.ie(command.ie())
				.im(command.im())
				.cnae(command.cnae())
				.taxRegime(command.taxRegime())
				.simplesOptante(command.simplesOptante())
				.sefazEnvironment(existing.getSefazEnvironment())
				.address(command.address())
				.state(command.state())
				.issuingEmail(command.issuingEmail())
				.phone(command.phone())
				.logoUrl(command.logoUrl())
				.parentCompanyId(command.parentCompanyId())
				.build();
	}

	/** The CNPJ identifies the legal entity: a different one is a new company, not an edit. Omitting it is fine. */
	private void rejectCnpjChange(final Company existing, final RegisterCompanyCommand command) {
		if (command.cnpj() != null && !command.cnpj().equals(existing.getCnpj())) {
			throw new BusinessRuleException(
					"CNPJ cannot be changed: it identifies the legal entity, register a new company instead");
		}
	}

	private void validateParentCompany(final CompanyId parentCompanyId) {
		if (parentCompanyId != null && companyRepositoryPort.findById(parentCompanyId).isEmpty()) {
			throw new BusinessRuleException("Parent company not found: " + parentCompanyId.value());
		}
	}

	private void createInitialDocumentSeries(final CompanyId companyId) {
		for (final FiscalDocumentType documentType : FiscalDocumentType.values()) {
			documentSeriesRepositoryPort.save(DocumentSeries.placeholder(companyId, documentType));
		}
	}
}
