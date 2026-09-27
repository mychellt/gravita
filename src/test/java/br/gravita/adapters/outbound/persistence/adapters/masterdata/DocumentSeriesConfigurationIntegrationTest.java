package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.mappers.masterdata.CompanyPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyCommand;
import br.gravita.core.usercases.ConfigureDocumentSeriesService;
import br.gravita.core.usercases.RegisterCompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GRA-51: independent QA verification of GRA-20's ConfigureDocumentSeriesUseCase
 * (PUT /api/companies/{id}/document-series/{type}). The controller-level tests
 * in CompanyControllerTest mock the use case, so they can't prove the real
 * business rules. This drives the actual RegisterCompanyService and
 * ConfigureDocumentSeriesService production classes against a real H2-backed
 * repository, covering what the mocked tests cannot: placeholder seeding on
 * registration, per-type counter independence, the forward-only nextNumber
 * rule once a series is live, and 404 for an unregistered company.
 *
 * NOTE: a full @SpringBootTest / HTTP-level equivalent of this test could not
 * be used because the application context currently fails to start -
 * TotpVerificationAdapter has two constructors and no @Autowired/no-arg
 * constructor, so Spring can't resolve which to use (see the QA verdict
 * comment on GRA-51 for the reproduction and stack trace). That is reported
 * separately as a blocker; this test exercises the real use case + repository
 * + database instead, which does not require the full context.
 */
@DataJpaTest
@Import({CompanyRepositoryAdapter.class, CompanyPersistenceMapperImpl.class,
		DocumentSeriesRepositoryAdapter.class, DocumentSeriesPersistenceMapperImpl.class})
class DocumentSeriesConfigurationIntegrationTest {

	@Autowired
	private CompanyRepositoryAdapter companyRepositoryAdapter;

	@Autowired
	private DocumentSeriesRepositoryAdapter documentSeriesRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private RegisterCompanyService registerCompanyService;
	private ConfigureDocumentSeriesService configureDocumentSeriesService;

	@BeforeEach
	void setUp() {
		registerCompanyService = new RegisterCompanyService(companyRepositoryAdapter, documentSeriesRepositoryAdapter);
		configureDocumentSeriesService = new ConfigureDocumentSeriesService(documentSeriesRepositoryAdapter);
	}

	@Test
	void registeringACompanySeedsAnUnconfiguredPlaceholderPerFiscalDocumentType() {
		CompanyId companyId = registerCompany();
		flushAndClear();

		for (FiscalDocumentType type : FiscalDocumentType.values()) {
			DocumentSeries seeded = documentSeriesRepositoryAdapter.findByCompanyIdAndDocumentType(companyId, type)
					.orElseThrow(() -> new AssertionError("expected a seeded placeholder for " + type));
			assertThat(seeded.getSeries()).isNull();
			assertThat(seeded.getNextNumber()).isEqualTo(1L);
		}
	}

	@Test
	void configuringOneDocumentTypeDoesNotAffectTheOthersForTheSameCompany() {
		CompanyId companyId = registerCompany();
		flushAndClear();

		configureDocumentSeriesService.execute(new ConfigureDocumentSeriesCommand(companyId, FiscalDocumentType.NFE, "001", 100L));
		flushAndClear();

		DocumentSeries nfe = documentSeriesRepositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE).orElseThrow();
		DocumentSeries nfce = documentSeriesRepositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFCE).orElseThrow();
		DocumentSeries nfse = documentSeriesRepositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFSE).orElseThrow();

		assertThat(nfe.getSeries()).isEqualTo("001");
		assertThat(nfe.getNextNumber()).isEqualTo(100L);
		assertThat(nfce.getSeries()).isNull();
		assertThat(nfce.getNextNumber()).isEqualTo(1L);
		assertThat(nfse.getSeries()).isNull();
		assertThat(nfse.getNextNumber()).isEqualTo(1L);
	}

	@Test
	void nextNumberCanMoveForwardOnceTheSeriesIsConfigured() {
		CompanyId companyId = registerCompany();
		flushAndClear();

		configureDocumentSeriesService.execute(new ConfigureDocumentSeriesCommand(companyId, FiscalDocumentType.NFE, "001", 100L));
		flushAndClear();
		configureDocumentSeriesService.execute(new ConfigureDocumentSeriesCommand(companyId, FiscalDocumentType.NFE, "001", 150L));
		flushAndClear();

		DocumentSeries persisted = documentSeriesRepositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE).orElseThrow();
		assertThat(persisted.getNextNumber()).isEqualTo(150L);
	}

	@Test
	void nextNumberCannotBeDecreasedOnceTheSeriesIsConfigured() {
		CompanyId companyId = registerCompany();
		flushAndClear();

		configureDocumentSeriesService.execute(new ConfigureDocumentSeriesCommand(companyId, FiscalDocumentType.NFE, "001", 100L));
		flushAndClear();

		assertThatThrownBy(() -> configureDocumentSeriesService.execute(
				new ConfigureDocumentSeriesCommand(companyId, FiscalDocumentType.NFE, "001", 50L)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("nextNumber cannot be decreased");

		DocumentSeries persisted = documentSeriesRepositoryAdapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE).orElseThrow();
		assertThat(persisted.getNextNumber()).isEqualTo(100L);
	}

	@Test
	void configuringSeriesForAnUnregisteredCompanyThrowsNotFound() {
		CompanyId unregistered = CompanyId.of(java.util.UUID.randomUUID());

		assertThatThrownBy(() -> configureDocumentSeriesService.execute(
				new ConfigureDocumentSeriesCommand(unregistered, FiscalDocumentType.NFE, "001", 100L)))
				.isInstanceOf(br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException.class);
	}

	private CompanyId registerCompany() {
		RegisterCompanyCommand command = new RegisterCompanyCommand(
				null,
				Document.cnpj("11222333000181"),
				"123456789",
				"987654",
				"6201-5/01",
				TaxRegime.SIMPLES_NACIONAL,
				true,
				"Rua Teste, 100",
				"SP",
				"nfe@example.com",
				"11999999999",
				null,
				null);
		return registerCompanyService.execute(command);
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
