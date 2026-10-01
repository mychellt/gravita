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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	@DisplayName("Seeds an unconfigured placeholder per fiscal document type when a company is registered")
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
	@DisplayName("Configuring one document type does not affect the others of the same company")
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
	@DisplayName("Allows the next number to move forward once the series is configured")
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
	@DisplayName("Rejects decreasing the next number once the series is configured")
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
	@DisplayName("Throws not found when configuring a series for an unregistered company")
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
