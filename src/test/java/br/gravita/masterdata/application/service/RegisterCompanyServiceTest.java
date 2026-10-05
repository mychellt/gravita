package br.gravita.masterdata.application.service;

import br.gravita.core.ports.inbound.masterdata.RegisterCompanyCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.usercases.RegisterCompanyService;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterCompanyServiceTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;

	@Test
	@DisplayName("Registers a new company with the homologation SEFAZ environment and initial document series")
	void shouldRegisterNewCompanyWithHomologationSefazEnvironmentAndInitialDocumentSeries() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CompanyId id = service.execute(newCompanyCommand(null));

		assertThat(id).isNotNull();
		ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		assertThat(savedCompany.getValue().getSefazEnvironment()).isEqualTo(SefazEnvironment.HOMOLOGATION);

		ArgumentCaptor<DocumentSeries> savedSeries = ArgumentCaptor.forClass(DocumentSeries.class);
		verify(documentSeriesRepositoryPort, times(4)).save(savedSeries.capture());
		Set<FiscalDocumentType> createdTypes = savedSeries.getAllValues().stream()
				.map(DocumentSeries::getDocumentType)
				.collect(java.util.stream.Collectors.toSet());
		assertThat(createdTypes).containsExactlyInAnyOrder(FiscalDocumentType.NFE, FiscalDocumentType.NFCE,
				FiscalDocumentType.NFSE, FiscalDocumentType.RPS);
	}

	@Test
	@DisplayName("Registers a branch against an existing parent company without error")
	void shouldRegisterBranchAgainstExistingParentCompanyWithoutError() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId parentId = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(parentId)).thenReturn(Optional.of(existingCompany(parentId)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CompanyId branchId = service.execute(newCompanyCommand(parentId));

		ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		assertThat(savedCompany.getValue().getParentCompanyId()).isEqualTo(parentId);
		assertThat(branchId).isNotNull();
	}

	@Test
	@DisplayName("Rejects a registration with an unknown parent company")
	void shouldRejectRegistrationWithUnknownParentCompany() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId unknownParentId = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(unknownParentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(newCompanyCommand(unknownParentId)))
				.isInstanceOf(BusinessRuleException.class);

		verify(companyRepositoryPort, never()).save(any());
		verify(documentSeriesRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Updates an existing company preserving its SEFAZ environment and skipping document series creation")
	void shouldUpdateExistingCompanyPreservingSefazEnvironmentAndSkipDocumentSeriesCreation() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId existingId = CompanyId.of(UUID.randomUUID());
		Company existing = Company.of(existingId, "Acme Ltda", VALID_CNPJ, "123456789", "987654", "6201-5/01",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.PRODUCTION, "Old address", "SP", "old@empresa.com",
				"11999999999", null, null);
		when(companyRepositoryPort.findById(existingId)).thenReturn(Optional.of(existing));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterCompanyCommand updateCommand = new RegisterCompanyCommand(existingId, "Acme Ltda", VALID_CNPJ, "123456789",
				"987654", "6201-5/01", TaxRegime.LUCRO_PRESUMIDO, false, "New address", "SP", "new@empresa.com",
				"11988888888", null, null);

		service.execute(updateCommand);

		ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		assertThat(savedCompany.getValue().getSefazEnvironment()).isEqualTo(SefazEnvironment.PRODUCTION);
		assertThat(savedCompany.getValue().getAddress()).isEqualTo("New address");
		verify(documentSeriesRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Updates the name and keeps the stored CNPJ when the command repeats it")
	void shouldUpdateNameWhenCommandRepeatsTheStoredCnpj() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId existingId = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(existingId)).thenReturn(Optional.of(existingCompany(existingId)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(updateCommand(existingId, "Novo Nome Ltda", VALID_CNPJ));

		ArgumentCaptor<Company> saved = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getName()).isEqualTo("Novo Nome Ltda");
		assertThat(saved.getValue().getCnpj()).isEqualTo(VALID_CNPJ);
	}

	@Test
	@DisplayName("Updates the other fields and keeps the stored CNPJ when the command omits it")
	void shouldKeepStoredCnpjWhenCommandOmitsIt() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId existingId = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(existingId)).thenReturn(Optional.of(existingCompany(existingId)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(updateCommand(existingId, "Novo Nome Ltda", null));

		ArgumentCaptor<Company> saved = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getCnpj()).isEqualTo(VALID_CNPJ);
		assertThat(saved.getValue().getName()).isEqualTo("Novo Nome Ltda");
		assertThat(saved.getValue().getAddress()).isEqualTo("Nova rua, 200");
	}

	@Test
	@DisplayName("Rejects an update that changes the CNPJ and leaves the record untouched")
	void shouldRejectUpdateThatChangesTheCnpj() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId existingId = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(existingId)).thenReturn(Optional.of(existingCompany(existingId)));

		assertThatThrownBy(() -> service.execute(updateCommand(existingId, "Novo Nome Ltda",
				Document.cnpj("11444777000161"))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CNPJ cannot be changed");

		verify(companyRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a registration without a name")
	void shouldRejectRegistrationWithoutName() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		RegisterCompanyCommand command = new RegisterCompanyCommand(null, " ", VALID_CNPJ, "123456789", "987654",
				"6201-5/01", TaxRegime.SIMPLES_NACIONAL, true, "Rua Teste, 100", "SP", "fiscal@empresa.com",
				"11999999999", null, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Name");
		verify(companyRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an update of an unknown company")
	void shouldRejectUpdateOfUnknownCompany() {
		RegisterCompanyService service = new RegisterCompanyService(companyRepositoryPort, documentSeriesRepositoryPort);
		CompanyId unknownId = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(unknownId)).thenReturn(Optional.empty());

		RegisterCompanyCommand updateCommand = new RegisterCompanyCommand(unknownId, "Acme Ltda", VALID_CNPJ, "123456789",
				"987654", "6201-5/01", TaxRegime.LUCRO_REAL, false, "Address", "SP", "email@empresa.com",
				"11988888888", null, null);

		assertThatThrownBy(() -> service.execute(updateCommand)).isInstanceOf(BusinessRuleException.class);
		verify(companyRepositoryPort, never()).save(any());
	}

	private RegisterCompanyCommand updateCommand(CompanyId id, String name, Document cnpj) {
		return new RegisterCompanyCommand(id, name, cnpj, "123456789", "987654", "6201-5/01",
				TaxRegime.SIMPLES_NACIONAL, true, "Nova rua, 200", "SP", "fiscal@empresa.com", "11999999999", null,
				null);
	}

	private RegisterCompanyCommand newCompanyCommand(CompanyId parentCompanyId) {
		return new RegisterCompanyCommand(null, "Acme Ltda", VALID_CNPJ, "123456789", "987654", "6201-5/01",
				TaxRegime.SIMPLES_NACIONAL, true, "Rua Teste, 100", "SP", "fiscal@empresa.com", "11999999999", null,
				parentCompanyId);
	}

	private Company existingCompany(CompanyId id) {
		return Company.of(id, "Acme Ltda", VALID_CNPJ, "123456789", "987654", "6201-5/01", TaxRegime.SIMPLES_NACIONAL, true,
				SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "fiscal@empresa.com", "11999999999", null,
				null);
	}
}
