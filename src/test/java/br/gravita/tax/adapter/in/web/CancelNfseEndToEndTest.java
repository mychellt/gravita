package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyId;
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
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import br.gravita.core.ports.outbound.tax.NfseCancellationRequest;
import br.gravita.core.ports.outbound.tax.NfseCancellationResult;
import br.gravita.core.ports.outbound.tax.NfseIssueRequest;
import br.gravita.core.ports.outbound.tax.NfseIssueResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Import(CancelNfseEndToEndTest.StubMunicipalityConfig.class)
class CancelNfseEndToEndTest {

	private static final String BETHA_CITY = "4205407";
	private static final String ISSNET_CITY = "3106200";
	private static final String ABRASF_CITY = "4106902";
	private static final Instant CANCELLED_AT = Instant.parse("2026-10-02T09:00:00Z");

	/** One stub per standard in the test context; a real deployment adds one {@code IssueNfsePort} per standard. */
	static class StubIssuer implements IssueNfsePort {

		Supplier<NfseCancellationResult> answer = () -> NfseCancellationResult.confirmed(CANCELLED_AT);
		NfseCancellationRequest lastRequest;

		@Override
		public NfseStandard standard() {
			return NfseStandard.BETHA;
		}

		@Override
		public NfseIssueResult issue(NfseIssueRequest request) {
			throw new UnsupportedOperationException("not exercised by the cancellation tests");
		}

		@Override
		public NfseCancellationResult cancel(NfseCancellationRequest request) {
			lastRequest = request;
			return answer.get();
		}
	}

	@TestConfiguration
	static class StubMunicipalityConfig {

		@Bean
		StubIssuer bethaIssuer() {
			return new StubIssuer();
		}
	}

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private MunicipalityIntegrationRepositoryPort integrationRepositoryPort;
	@Autowired
	private NfseRepositoryPort nfseRepositoryPort;
	@Autowired
	private NfseJpaRepository nfseJpaRepository;
	@Autowired
	private StubIssuer bethaIssuer;

	private long rpsCounter;

	@BeforeEach
	void reset() {
		bethaIssuer.lastRequest = null;
		bethaIssuer.answer = () -> NfseCancellationResult.confirmed(CANCELLED_AT);
	}

	private void register(String ibge, NfseStandard standard, boolean homologated) {
		integrationRepositoryPort.save(MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()),
				ibge, standard, homologated ? "1.0" : null, homologated ? "https://nfse.example/ws" : null,
				CertificateType.A1, List.of("inscricaoMunicipal"), homologated));
	}

	private NfseDocument rps(String municipality) {
		return nfseRepositoryPort.save(NfseDocument.issueRps(NfseId.of(UUID.randomUUID()),
				CompanyId.of(UUID.randomUUID()), municipality,
				NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null),
				ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, municipality, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal("50.00"), null, List.of(), "Consultoria", "RPS",
				++rpsCounter, Instant.now()));
	}

	private NfseDocument draftDocument(String municipality) {
		NfseDocument rps = rps(municipality);
		return nfseRepositoryPort.save(rps.convertToNfse("1", rps.getRpsNumber(), Instant.now()));
	}

	private UUID draft(String municipality) {
		return draftDocument(municipality).getId().value();
	}

	private UUID authorized(String municipality) {
		NfseDocument sent = draftDocument(municipality).send(Instant.now());
		return nfseRepositoryPort.save(sent.authorize("PROT-123", Instant.parse("2026-10-01T12:00:00Z"), "xml/ref-1"))
				.getId().value();
	}

	private NfseJpaEntity persisted(UUID id) {
		return nfseJpaRepository.findById(id).orElseThrow();
	}

	private ResultActions cancel(UUID id, String body) throws Exception {
		return mockMvc.perform(post("/api/nfse/{id}/cancel", id).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions cancel(UUID id) throws Exception {
		return cancel(id, "{\"justification\":\"Servico nao prestado\"}");
	}

	@Test
	@DisplayName("Cancels an authorized NFS-e through its municipality's standard adapter and keeps the record")
	void anAuthorizedDocumentIsCancelledThroughItsStandardsAdapterAndTheRecordIsRetained() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = authorized(BETHA_CITY);

		cancel(id).andExpect(status().isNoContent());

		NfseJpaEntity cancelled = persisted(id);
		assertThat(cancelled.getStatus()).isEqualTo(NfseStatus.CANCELLED);
		assertThat(cancelled.getCancellationJustification()).isEqualTo("Servico nao prestado");
		assertThat(cancelled.getCancelledAt()).isEqualTo(CANCELLED_AT);
		assertThat(cancelled.getProtocol()).isEqualTo("PROT-123");
		assertThat(cancelled.getXmlReference()).isEqualTo("xml/ref-1");
		assertThat(bethaIssuer.lastRequest.integration().getStandard()).isEqualTo(NfseStandard.BETHA);
		assertThat(bethaIssuer.lastRequest.justification()).isEqualTo("Servico nao prestado");
		assertThat(nfseJpaRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("Refuses a missing or blank justification with a client error and sends nothing to the municipality")
	void aMissingOrBlankJustificationIs400OrRefusedAndNothingIsSent() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = authorized(BETHA_CITY);

		cancel(id, "{}").andExpect(status().isBadRequest());
		cancel(id, "{\"justification\":\"   \"}").andExpect(status().isBadRequest());

		assertThat(persisted(id).getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
		assertThat(bethaIssuer.lastRequest).isNull();
	}

	@Test
	@DisplayName("Returns 409 with the municipality's reason when it refuses the cancellation, leaving the document authorized")
	void aMunicipalityRefusalIs409WithItsReasonAndTheDocumentStaysAuthorized() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = authorized(BETHA_CITY);
		bethaIssuer.answer = () -> NfseCancellationResult.rejected("E79 - Prazo de cancelamento expirado");

		cancel(id).andExpect(status().isConflict())
				.andExpect(jsonPath("$").value(containsString("E79 - Prazo de cancelamento expirado")));

		NfseJpaEntity untouched = persisted(id);
		assertThat(untouched.getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
		assertThat(untouched.getCancellationJustification()).isNull();
	}

	@Test
	@DisplayName("Returns 503 and leaves the document authorized when the municipality does not answer")
	void aMunicipalityThatDoesNotAnswerIs503AndTheDocumentStaysAuthorized() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = authorized(BETHA_CITY);
		bethaIssuer.answer = () -> {
			throw new NfseMunicipalityUnavailableException("Municipality did not answer in time", null);
		};

		cancel(id).andExpect(status().isServiceUnavailable());

		assertThat(persisted(id).getStatus()).isEqualTo(NfseStatus.AUTHORIZED);
	}

	@Test
	@DisplayName("Refuses a second cancellation of an NFS-e that was already cancelled")
	void aSecondCancellationOfTheSameDocumentIsRefused() throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		UUID id = authorized(BETHA_CITY);
		cancel(id).andExpect(status().isNoContent());
		bethaIssuer.lastRequest = null;

		cancel(id).andExpect(status().isConflict()).andExpect(jsonPath("$").value(containsString("CANCELLED")));

		assertThat(bethaIssuer.lastRequest).isNull();
	}

	@Test
	@DisplayName("Refuses cancelling drafts, RPS, unregistered municipalities and standards without an adapter, and returns 404 for an unknown id")
	void aDraftAnRpsAnUnregisteredMunicipalityAndAStandardWithoutAnAdapterAreRefusedAndAnUnknownIdIs404()
			throws Exception {
		register(BETHA_CITY, NfseStandard.BETHA, true);
		register(ABRASF_CITY, NfseStandard.ABRASF, true);
		register(ISSNET_CITY, NfseStandard.ISSNET, false);

		UUID draft = draft(BETHA_CITY);
		cancel(draft).andExpect(status().isConflict()).andExpect(jsonPath("$").value(containsString("DRAFT")));
		assertThat(persisted(draft).getStatus()).isEqualTo(NfseStatus.DRAFT);

		cancel(rps(BETHA_CITY).getId().value()).andExpect(status().isConflict());

		cancel(authorized("3304557")).andExpect(status().isConflict())
				.andExpect(jsonPath("$").value(containsString("3304557")));
		cancel(authorized(ABRASF_CITY)).andExpect(status().isConflict())
				.andExpect(jsonPath("$").value(containsString("ABRASF")));
		cancel(authorized(ISSNET_CITY)).andExpect(status().isConflict())
				.andExpect(jsonPath("$").value(containsString("homologated")));
		assertThat(bethaIssuer.lastRequest).isNull();

		cancel(UUID.randomUUID()).andExpect(status().isNotFound());
	}
}
