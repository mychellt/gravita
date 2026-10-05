package br.gravita.masterdata.application.service;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.usercases.GetCompanyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCompanyServiceTest {

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@InjectMocks
	private GetCompanyService service;

	@Test
	@DisplayName("Returns the stored company")
	void shouldReturnTheStoredCompany() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		final Company company = Company.builder()
				.id(id)
				.name("Acme Ltda")
				.cnpj(Document.cnpj("11222333000181"))
				.ie("123456789")
				.im("987654")
				.cnae("6201-5/01")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("nfe@acme.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(company));

		assertThat(service.execute(id)).isSameAs(company);
	}

	@Test
	@DisplayName("Fails with company-not-found for an unknown id")
	void shouldFailForAnUnknownCompany() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(id)).isInstanceOf(CompanyNotFoundException.class)
				.hasMessageContaining(id.value().toString());
	}
}
