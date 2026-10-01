package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseMunicipalityUnavailableException;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import br.gravita.core.ports.outbound.tax.NfseCancellationRequest;
import br.gravita.core.ports.outbound.tax.NfseCancellationResult;
import br.gravita.core.ports.outbound.tax.NfseIssueRequest;
import br.gravita.core.ports.outbound.tax.NfseIssueResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Import(TransmitNfseEndToEndTest.StubMunicipalityConfig.class)
class TransmitNfseEndToEndTest {

	private static final String BETHA_CITY = "4205407";
	private static final String ISSNET_CITY = "3106200";
	private static final String ABRASF_CITY = "4106902";
	private static final byte[] SIGNED_XML = "<GerarNfseEnvio>signed</GerarNfseEnvio>"
			.getBytes(StandardCharsets.UTF_8);

	/** One stub per standard in the test context; a real deployment adds one {@code IssueNfsePort} per standard. */
	static class StubIssuer implements IssueNfsePort {

		private final NfseStandard standard;
		Supplier<NfseIssueResult> answer = () -> NfseIssueResult.authorized("PROT-123",
				Instant.parse("2026-10-01T12:00:00Z"), SIGNED_XML);
		NfseIssueRequest lastRequest;

		StubIssuer(NfseStandard standard) {
			this.standard = standard;
		}

		@Override
		public NfseStandard standard() {
			return standard;
		}

		@Override
		public NfseIssueResult issue(NfseIssueRequest request) {
			lastRequest = request;
			return answer.get();
		}

		@Override
		public NfseCancellationResult cancel(NfseCancellationRequest request) {
			throw new UnsupportedOperationException("not exercised by the transmission tests");
		}
	}

	@TestConfiguration
	static class StubMunicipalityConfig {

		@Bean
		StubIssuer bethaIssuer() {
			return new StubIssuer(NfseStandard.BETHA);
		}
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;
	@Autowired
	private MunicipalityIntegrationRepositoryPort integrationRepositoryPort;
	@Autowired
	private NfseRepositoryPort nfseRepositoryPort;
	@Autowired
	private NfseJpaRepository nfseJpaRepository;
	@Autowired
	private XmlObjectStoragePort xmlObjectStoragePort;
	@Autowired
	private StubIssuer bethaIssuer;

	private CompanyId companyId;
	private long rpsCounter;

	@BeforeEach
	void seed() {
		companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, Document.cnpj("11222333000181"), "123456789", "987654",
				"6201500", TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfse@example.com", "11999999999", null, null));
		bethaIssuer.lastRequest = null;
		bethaIssuer.answer = () -> NfseIssueResult.authorized("PROT-123", Instant.parse("2026-10-01T12:00:00Z"),
				SIGNED_XML);
	}

	private void register(String ibge, NfseStandard standard, boolean homologated) {
		integrationRepositoryPort.save(MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()),
				ibge, standard, homologated ? "1.0" : null, homologated ? "https://nfse.example/ws" : null,
				CertificateType.A1, List.of("inscricaoMunicipal"), homologated));
	}

	private NfseDocument rps(String municipality) {
		return nfseRepositoryPort.save(NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), companyId, municipality,
				NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null),
				ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, municipality, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal("50.00"), null, List.of(), "Consultoria", "RPS",
				++rpsCounter, Instant.now()));
	}

	private UUID draft(String municipality) {
		NfseDocument rps = rps(municipality);
		return nfseRepositoryPort.save(rps.convertToNfse("1", rps.getRpsNumber(), Instant.now())).getId().value();
	}

	private NfseJpaEntity persisted(UUID id) {
		return nfseJpaRepository.findById(id).orElseThrow();
	}

	private org.springframework.test.web.servlet.ResultActions transmit(UUID id) throws Exception {
		return mockMvc.perform(post("/api/nfse/{id}/transmit", id));
	}

	@Test
	void aHomologatedMunicipalityIsTransmittedAndTheDocumentIsAuthorizedWithOnlyAnXmlReference() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = draft(BETHA_CITY);

		transmit(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("AUTHORIZED"))
				.andExpect(jsonPath("$.protocol").value("PROT-123"))
				.andExpect(jsonPath("$.authorizedAt").exists())
				.andExpect(jsonPath("$.xml").doesNotExist());

		NfseJpaEntity authorized = persisted(id);
		assertThat(authorized.getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
		assertThat(authorized.getProtocol()).isEqualTo("PROT-123");
		assertThat(authorized.getAuthorizedAt()).isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
		assertThat(authorized.getSentAt()).isNotNull();
		assertThat(xmlObjectStoragePort.retrieve(authorized.getXmlReference())).isEqualTo(SIGNED_XML);
		assertThat(bethaIssuer.lastRequest.integration().getStandard()).isEqualTo(NfseStandard.BETHA);
	}

	@Test
	void aRejectionIsReportedKeepsTheDocumentADraftAndAllowsATransmitOnceFixed() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = draft(BETHA_CITY);
		bethaIssuer.answer = () -> NfseIssueResult.rejected("E160 - Arquivo em desacordo com o XML Schema");

		transmit(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("REJECTED"))
				.andExpect(jsonPath("$.rejectionReason").value("E160 - Arquivo em desacordo com o XML Schema"))
				.andExpect(jsonPath("$.protocol").doesNotExist());

		NfseJpaEntity rejected = persisted(id);
		assertThat(rejected.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(rejected.getLastRejectionReason()).startsWith("E160");
		assertThat(rejected.getXmlReference()).isNull();

		bethaIssuer.answer = () -> NfseIssueResult.authorized("PROT-2", Instant.now(), SIGNED_XML);
		transmit(id).andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("AUTHORIZED"));
		assertThat(persisted(id).getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
		assertThat(persisted(id).getLastRejectionReason()).isNull();
	}

	@Test
	void aNonHomologatedMunicipalityGetsTheXmlAndInstructionsAndTheDocumentIsUntouched() throws Exception {
		register(ISSNET_CITY, NfseStandard.ISSNET, false);
		UUID id = draft(ISSNET_CITY);

		transmit(id).andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("MANUAL_UPLOAD_REQUIRED"))
				.andExpect(jsonPath("$.xml").value(containsString("<Cnpj>11222333000181</Cnpj>")))
				.andExpect(jsonPath("$.xml").value(containsString("<ValorServicos>1000.00</ValorServicos>")))
				.andExpect(jsonPath("$.instructions").value(containsString("ISSNET")))
				.andExpect(jsonPath("$.protocol").doesNotExist());

		NfseJpaEntity untouched = persisted(id);
		assertThat(untouched.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(untouched.getSentAt()).isNull();
		assertThat(untouched.getXmlReference()).isNull();
		assertThat(bethaIssuer.lastRequest).isNull();
	}

	@Test
	void aMunicipalityThatDoesNotAnswerIs503() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = draft(BETHA_CITY);
		bethaIssuer.answer = () -> {
			throw new NfseMunicipalityUnavailableException("Municipality did not answer in time", null);
		};

		transmit(id).andExpect(status().isServiceUnavailable());
	}

	@Test
	void aHomologatedStandardWithoutAnAdapterIs409AndTheDocumentStaysADraft() throws Exception {
		register(ABRASF_CITY, NfseStandard.ABRASF, true);
		UUID id = draft(ABRASF_CITY);

		transmit(id).andExpect(status().isConflict()).andExpect(jsonPath("$").value(containsString("ABRASF")));

		assertThat(persisted(id).getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(persisted(id).getSentAt()).isNull();
	}

	@Test
	void anAuthorizedAnRpsAndAnUnregisteredMunicipalityAreRefusedAndAnUnknownIdIs404() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID authorized = draft(BETHA_CITY);
		transmit(authorized).andExpect(status().isOk());
		transmit(authorized).andExpect(status().isConflict());

		UUID rps = rps(BETHA_CITY).getId().value();
		transmit(rps).andExpect(status().isConflict());

		UUID unregistered = draft("3304557");
		transmit(unregistered).andExpect(status().isConflict()).andExpect(jsonPath("$").value(containsString("3304557")));

		transmit(UUID.randomUUID()).andExpect(status().isNotFound());
	}
}
