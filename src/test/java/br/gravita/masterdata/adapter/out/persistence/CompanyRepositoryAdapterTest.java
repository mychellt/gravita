package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.Company;
import br.gravita.masterdata.domain.model.CompanyId;
import br.gravita.masterdata.domain.model.SefazEnvironment;
import br.gravita.masterdata.domain.model.TaxRegime;
import br.gravita.shared.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(CompanyRepositoryAdapter.class)
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
