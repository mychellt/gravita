package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalServiceCodeJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.ServiceTaxRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalServiceCodeJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.ServiceTaxRuleJpaRepository;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.WithholdingMode;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class IssueRpsEndToEndTest {

	private static final String SP = "3550308";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;
	@Autowired
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;
	@Autowired
	private NfseJpaRepository nfseJpaRepository;
	@Autowired
	private ServiceTaxRuleJpaRepository serviceTaxRuleJpaRepository;
	@Autowired
	private MunicipalServiceCodeJpaRepository municipalServiceCodeJpaRepository;

	private CompanyId companyId;

	@BeforeEach
	void seed() {
		companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, Document.cnpj("11222333000181"), "123456789", "987654",
				"6201500", TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfse@example.com", "11999999999", null, null));
		documentSeriesRepositoryPort
				.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE).reconfigure("001", 500L));
		documentSeriesRepositoryPort
				.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.RPS).reconfigure("RPS", 1L));
		rule(SP, TaxType.ISS, "5.0000", WithholdingMode.TOMADOR_COMPANY);
		rule(null, TaxType.PIS, "0.6500", WithholdingMode.TOMADOR_COMPANY);
		rule(null, TaxType.INSS, "11.0000", WithholdingMode.NEVER);
	}

	private void rule(String municipality, TaxType type, String rate, WithholdingMode mode) {
		serviceTaxRuleJpaRepository.save(ServiceTaxRuleJpaEntity.builder().id(UUID.randomUUID())
				.serviceCode("01.05").municipalityIbge(municipality).taxType(type)
				.ratePercentage(new BigDecimal(rate)).withholding(mode).build());
	}

	private String body(String tomador, String extra) {
		return """
				{
				  "providerCompanyId": "%s",
				  "providerMunicipalityIbgeCode": "%s",
				  "tomador": %s,
				  "serviceCode": "1.05",
				  "placeOfProvision": "PROVIDER",
				  "serviceAmount": 1000.00,
				  "discrimination": "Desenvolvimento de software"%s
				}
				""".formatted(companyId.value(), SP, tomador, extra);
	}

	private static final String PJ_FULL = """
			{ "document": "11.222.333/0001-81", "personType": "COMPANY", "name": "Tomador SA",
			  "municipalityIbgeCode": "3550308",
			  "address": { "street": "Rua A", "number": "10", "neighborhood": "Centro", "zipCode": "01001000",
			               "state": "SP" } }
			""";
	private static final String PJ_NO_ADDRESS = """
			{ "document": "11222333000181", "personType": "COMPANY", "name": "Tomador SA" }
			""";
	private static final String PF = """
			{ "document": "529.982.247-25", "personType": "INDIVIDUAL", "name": "Pessoa Fisica" }
			""";

	@Test
	void issuesAnRpsWithItsOwnSeriesAndTheResolvedIssAndWithholdings() throws Exception {
		String response = mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content(body(PJ_FULL, "")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();

		UUID id = UUID.fromString(response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
		NfseJpaEntity persisted = nfseJpaRepository.findById(id).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(NfseStatus.RPS);
		assertThat(persisted.getRpsSeries()).isEqualTo("RPS");
		assertThat(persisted.getRpsNumber()).isEqualTo(1L);
		assertThat(persisted.getServiceCode()).isEqualTo("01.05");
		assertThat(persisted.getIssRate()).isEqualByComparingTo("5.0000");
		assertThat(persisted.getIssAmount()).isEqualByComparingTo("50.00");
		assertThat(persisted.getTomadorDocument()).isEqualTo("11222333000181");
		assertThat(persisted.getWithholdings()).extracting(w -> w.getTaxType())
				.containsExactlyInAnyOrder(TaxType.ISS, TaxType.PIS);
		assertThat(persisted.getDiscrimination()).isEqualTo("Desenvolvimento de software");
	}

	@Test
	void ac5_consecutiveRpsGetConsecutiveNumbersAndLeaveTheNfeSeriesUntouched() throws Exception {
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON).content(body(PF, "")))
					.andExpect(status().isCreated());
		}

		assertThat(nfseJpaRepository.findAll()).extracting(NfseJpaEntity::getRpsNumber)
				.containsExactlyInAnyOrder(1L, 2L);
		DocumentSeries rps = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.RPS).orElseThrow();
		DocumentSeries nfe = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE).orElseThrow();
		assertThat(rps.getNextNumber()).isEqualTo(3L);
		assertThat(nfe.getNextNumber()).isEqualTo(500L);
	}

	@Test
	void ac2_aWithholdingTomadorWithoutAFullAddressIsRejectedWith409AndNothingIsPersisted() throws Exception {
		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content(body(PJ_NO_ADDRESS, "")))
				.andExpect(status().isConflict());

		assertThat(nfseJpaRepository.count()).isZero();
		assertThat(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.RPS)
				.orElseThrow().getNextNumber()).isEqualTo(1L);
	}

	@Test
	void ac3_anOverrideNeedsAJustificationAndIsRecordedWhenGiven() throws Exception {
		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content(body(PF, ", \"issRateOverride\": 3.0")))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content(body(PF, ", \"issRateOverride\": 3.0, \"overrideJustification\": \"Incentivo\"")))
				.andExpect(status().isCreated());

		NfseJpaEntity persisted = nfseJpaRepository.findAll().get(0);
		assertThat(persisted.getIssRate()).isEqualByComparingTo("3.0");
		assertThat(persisted.getIssAmount()).isEqualByComparingTo("30.00");
		assertThat(persisted.getIssRateOverrideJustification()).isEqualTo("Incentivo");
	}

	@Test
	void ac1_aCodeMissingFromTheMunicipalListIsRejected() throws Exception {
		MunicipalServiceCodeJpaEntity other = MunicipalServiceCodeJpaEntity.builder().id(UUID.randomUUID())
				.municipalityIbge(SP).serviceCode("02.01").build();
		municipalServiceCodeJpaRepository.save(other);

		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON).content(body(PF, "")))
				.andExpect(status().isConflict());

		municipalServiceCodeJpaRepository.save(MunicipalServiceCodeJpaEntity.builder().id(UUID.randomUUID())
				.municipalityIbge(SP).serviceCode("01.05").build());
		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON).content(body(PF, "")))
				.andExpect(status().isCreated());
	}

	@Test
	void ac1_aCodeOutsideLc116IsRejected() throws Exception {
		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content(body(PF, "").replace("\"1.05\"", "\"99.99\"")))
				.andExpect(status().isConflict());
	}

	@Test
	void anUnknownProviderIs404AndAnIncompleteBodyIs400() throws Exception {
		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content(body(PF, "").replace(companyId.value().toString(), UUID.randomUUID().toString())))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/nfse/rps").contentType(MediaType.APPLICATION_JSON)
						.content("{ \"serviceCode\": \"1.05\" }"))
				.andExpect(status().isBadRequest());
	}
}
