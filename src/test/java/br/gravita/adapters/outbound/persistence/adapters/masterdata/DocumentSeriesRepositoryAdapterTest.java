package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({DocumentSeriesRepositoryAdapter.class, DocumentSeriesPersistenceMapperImpl.class})
class DocumentSeriesRepositoryAdapterTest {

	@Autowired
	private DocumentSeriesRepositoryAdapter repositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private CompanyId persistCompany() {
		UUID id = UUID.randomUUID();
		entityManager.persist(CompanyJpaEntity.builder()
				.id(id)
				.cnpj(UUID.randomUUID().toString().substring(0, 14))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.issuingEmail("nfe@example.com")
				.phone("11999999999")
				.build());
		return CompanyId.of(id);
	}

	@Test
	void shouldSaveAndRetrieveByCompanyAndDocumentType() {
		CompanyId companyId = persistCompany();
		DocumentSeries placeholder = DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE);

		repositoryAdapter.save(placeholder);

		Optional<DocumentSeries> found = repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE);
		assertThat(found).isPresent();
		assertThat(found.get().getNextNumber()).isEqualTo(1L);
		assertThat(found.get().getSeries()).isNull();
	}

	@Test
	void shouldReturnEmptyWhenNoSeriesConfiguredForType() {
		CompanyId companyId = persistCompany();

		assertThat(repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFCE)).isEmpty();
	}

	@Test
	void shouldKeepEachDocumentTypeIndependentForTheSameCompany() {
		CompanyId companyId = persistCompany();
		repositoryAdapter.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE).reconfigure("001", 100L));
		repositoryAdapter.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFCE).reconfigure("A", 1L));

		assertThat(repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE)).get()
				.extracting(DocumentSeries::getNextNumber).isEqualTo(100L);
		assertThat(repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFCE)).get()
				.extracting(DocumentSeries::getNextNumber).isEqualTo(1L);
		assertThat(repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFSE)).isEmpty();
	}

	@Test
	void reconfiguringAndSavingAgainImmediatelyReflectsOnNextRead() {
		CompanyId companyId = persistCompany();
		DocumentSeries configured = DocumentSeries.placeholder(companyId, FiscalDocumentType.NFSE).reconfigure("001", 10L);
		DocumentSeries saved = repositoryAdapter.save(configured);

		DocumentSeries reconfigured = saved.reconfigure("001", 20L);
		repositoryAdapter.save(reconfigured);

		Optional<DocumentSeries> found = repositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFSE);
		assertThat(found).isPresent();
		assertThat(found.get().getNextNumber()).isEqualTo(20L);
	}
}
