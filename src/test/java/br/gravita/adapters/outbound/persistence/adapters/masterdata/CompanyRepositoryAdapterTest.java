package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.mappers.masterdata.CompanyPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({CompanyRepositoryAdapter.class, CompanyPersistenceMapperImpl.class})
class CompanyRepositoryAdapterTest {

	@Autowired
	private CompanyRepositoryAdapter repositoryAdapter;

	@Test
	void shouldPersistNewCompanyWithApplicationAssignedId() {
		Company company = newCompany(CompanyId.of(UUID.randomUUID()));

		Company saved = repositoryAdapter.save(company);

		assertThat(repositoryAdapter.findById(saved.getId())).isPresent().get()
				.satisfies(found -> assertThat(found.getCnpj()).isEqualTo(company.getCnpj()));
	}

	@Test
	void shouldUpdateExistingCompanyWithoutLosingCreatedAt() {
		Company company = newCompany(CompanyId.of(UUID.randomUUID()));
		Company saved = repositoryAdapter.save(company);

		Company changed = Company.of(saved.getId(), saved.getCnpj(), saved.getIe(), saved.getIm(), saved.getCnae(),
				TaxRegime.LUCRO_REAL, saved.isSimplesOptante(), saved.getSefazEnvironment(), "New address",
				saved.getIssuingEmail(), saved.getPhone(), saved.getLogoUrl(), saved.getParentCompanyId());

		repositoryAdapter.save(changed);

		assertThat(repositoryAdapter.findById(saved.getId())).isPresent().get()
				.satisfies(found -> assertThat(found.getTaxRegime()).isEqualTo(TaxRegime.LUCRO_REAL));
	}

	private Company newCompany(CompanyId id) {
		return Company.of(id, Document.cnpj("11222333000181"), "123456789", "987654", "6201-5/01",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100",
				"fiscal@empresa.com", "11999999999", null, null);
	}
}
