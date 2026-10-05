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

	private Company existingCompany(final CompanyId id, final SefazEnvironment environment) {
		return Company.builder()
				.id(id)
				.name("Acme Ltda")
				.cnpj(VALID_CNPJ)
				.ie("123456789")
				.im("987654")
				.cnae("6201-5/01")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(environment)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("fiscal@empresa.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}

	@Test
	@DisplayName("Switches a company from homologation to production")
	void shouldSwitchCompanyFromHomologationToProduction() {
		final SwitchSefazEnvironmentService service = new SwitchSefazEnvironmentService(companyRepositoryPort);
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id, SefazEnvironment.HOMOLOGATION)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new SwitchSefazEnvironmentCommand(id, SefazEnvironment.PRODUCTION));

		final ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		final Company saved = savedCompany.getValue();
		assertThat(saved.getSefazEnvironment()).isEqualTo(SefazEnvironment.PRODUCTION);
		assertThat(saved.getId()).isEqualTo(id);
		assertThat(saved.getCnpj()).isEqualTo(VALID_CNPJ);
	}

	@Test
	@DisplayName("Switches a company from production to homologation")
	void shouldSwitchCompanyFromProductionToHomologation() {
		final SwitchSefazEnvironmentService service = new SwitchSefazEnvironmentService(companyRepositoryPort);
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(existingCompany(id, SefazEnvironment.PRODUCTION)));
		when(companyRepositoryPort.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new SwitchSefazEnvironmentCommand(id, SefazEnvironment.HOMOLOGATION));

		final ArgumentCaptor<Company> savedCompany = ArgumentCaptor.forClass(Company.class);
		verify(companyRepositoryPort).save(savedCompany.capture());
		assertThat(savedCompany.getValue().getSefazEnvironment()).isEqualTo(SefazEnvironment.HOMOLOGATION);
	}

	@Test
	@DisplayName("Throws when the company does not exist")
	void shouldThrowWhenCompanyDoesNotExist() {
		final SwitchSefazEnvironmentService service = new SwitchSefazEnvironmentService(companyRepositoryPort);
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new SwitchSefazEnvironmentCommand(id, SefazEnvironment.PRODUCTION)))
				.isInstanceOf(CompanyNotFoundException.class);

		verify(companyRepositoryPort, never()).save(any());
	}
}
