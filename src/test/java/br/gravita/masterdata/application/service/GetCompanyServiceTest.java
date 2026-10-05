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
		CompanyId id = CompanyId.of(UUID.randomUUID());
		Company company = Company.of(id, "Acme Ltda", Document.cnpj("11222333000181"), "123456789", "987654",
				"6201-5/01", TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@acme.com", "11999999999", null, null);
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.of(company));

		assertThat(service.execute(id)).isSameAs(company);
	}

	@Test
	@DisplayName("Fails with company-not-found for an unknown id")
	void shouldFailForAnUnknownCompany() {
		CompanyId id = CompanyId.of(UUID.randomUUID());
		when(companyRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(id)).isInstanceOf(CompanyNotFoundException.class)
				.hasMessageContaining(id.value().toString());
	}
}
