package br.gravita.masterdata.application.service;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.usercases.SwitchSefazEnvironmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwitchSefazEnvironmentServiceTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	private Company existingCompany(CompanyId id, SefazEnvironment environment) {
		return Company.of(id, "Acme Ltda", VALID_CNPJ, "123456789", "987654", "6201-5/01", TaxRegime.SIMPLES_NACIONAL, true,
				environment, "Rua Teste, 100", "SP", "fiscal@empresa.com", "11999999999", null, null);
	}

	@Test
	@DisplayName("Switches a company from homologation to production")
	void shouldSwitchCompanyFromHomologationToProduction() {
		SwitchSefazEnvironmentService service = new SwitchSefazEnvironmentService(companyRepositoryPort);
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id, SefazEnvironment.HOMOLOGATION)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new SwitchSefazEnvironmentCommand(id, SefazEnvironment.PRODUCTION));

		ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		Company saved = savedCompany.getValue();
		assertThat(saved.getSefazEnvironment()).isEqualTo(SefazEnvironment.PRODUCTION);
		assertThat(saved.getId()).isEqualTo(id);
		assertThat(saved.getCnpj()).isEqualTo(VALID_CNPJ);
	}

	@Test
	@DisplayName("Switches a company from production to homologation")
	void shouldSwitchCompanyFromProductionToHomologation() {
		SwitchSefazEnvironmentService service = new SwitchSefazEnvironmentService(companyRepositoryPort);
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id, SefazEnvironment.PRODUCTION)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new SwitchSefazEnvironmentCommand(id, SefazEnvironment.HOMOLOGATION));

		ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		assertThat(savedCompany.getValue().getSefazEnvironment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
	}

	@Test
	@DisplayName("Throws when the company does not exist")
	void shouldThrowWhenCompanyDoesNotExist() {
		SwitchSefazEnvironmentService service = new SwitchSefazEnvironmentService(companyRepositoryPort);
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new SwitchSefazEnvironmentCommand(id, SefazEnvironment.PRODUCTION)))
				.isInstanceOf(CompanyNotFoundException.class);

		verify(companyRepositoryPort, never()).save(any());
	}
}
