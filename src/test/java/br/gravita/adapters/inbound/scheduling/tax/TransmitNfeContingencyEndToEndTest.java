package br.gravita.adapters.inbound.scheduling.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransmissionQueueEntry;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class TransmitNfeContingencyEndToEndTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";
	private static HttpServer mockContingencyServer;

	@Autowired
	private TransmissionQueueConsumer transmissionQueueConsumer;

	@Autowired
	private NfeRepositoryPort nfeRepositoryPort;

	@Autowired
	private TransmissionQueuePort transmissionQueuePort;

	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;

	@Autowired
	private CertificateStoragePort certificateStoragePort;

	private CompanyId companyId;

	@DynamicPropertySource
	static void sefazBaseUrls(DynamicPropertyRegistry registry) throws Exception {
		mockContingencyServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		mockContingencyServer.createContext("/nfce/autorizacao", exchange -> {
			byte[] body = "{\"protocol\":\"svc-e2e-1\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		mockContingencyServer.start();
		int port = mockContingencyServer.getAddress().getPort();

		// SEFAZ-UF itself is deliberately unreachable (connection refused) so every
		// attempt against it fails fast as SefazUnavailableException - only the
		// SVC-AN/SVC-RS contingency endpoint below can ever answer.
		registry.add("gravita.sefaz.base-url.homologation", () -> "http://localhost:1");
		registry.add("gravita.sefaz.base-url.contingency", () -> "http://localhost:" + port);
	}

	@AfterAll
	static void stopServer() {
		if (mockContingencyServer != null) {
			mockContingencyServer.stop(0);
		}
	}

	@BeforeEach
	void seedIssuanceInfrastructure() throws Exception {
		companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, Document.cnpj(VALID_CNPJ), "123456789", "987654",
				"6201500", br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL, true,
				SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfe@example.com", "11999999999", null, null));
		certificateStoragePort.save(DigitalCertificate.upload(companyId, CertificateType.A1, loadCertificateFixture(),
				"gravita-test-pass", Instant.now().plusSeconds(3600)));
	}

	@Test
	void ac3_switchesToSvcContingencyAfterRepeatedSefazUfTimeoutsAndThenAuthorizesThroughIt() {
		NfeDocumentId documentId = NfeDocumentId.of(UUID.randomUUID());
		NfeDocument queued = queuedDocument(documentId);
		nfeRepositoryPort.save(queued);
		transmissionQueuePort.enqueue(documentId);

		for (int attempts = 0; attempts < TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS; attempts++) {
			transmissionQueueConsumer.processEntry(new TransmissionQueueEntry(documentId.value(), attempts,
					Instant.now()));

			NfeDocument afterAttempt = nfeRepositoryPort.findById(documentId).orElseThrow();
			assertThat(afterAttempt.getStatus()).isEqualTo(NfeDocumentStatus.SENT);
			boolean shouldHaveSwitched = attempts + 1 >= TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS;
			assertThat(afterAttempt.isContingencyMode()).isEqualTo(shouldHaveSwitched);
		}

		transmissionQueueConsumer.processEntry(new TransmissionQueueEntry(documentId.value(),
				TransmissionQueueConsumer.CONTINGENCY_THRESHOLD_ATTEMPTS, Instant.now()));

		NfeDocument authorized = nfeRepositoryPort.findById(documentId).orElseThrow();
		assertThat(authorized.getStatus()).isEqualTo(NfeDocumentStatus.AUTHORIZED);
		assertThat(authorized.getSefazProtocol()).isEqualTo("svc-e2e-1");
		assertThat(authorized.isContingencyMode()).isTrue();
	}

	private NfeDocument queuedDocument(NfeDocumentId documentId) {
		UUID productId = UUID.randomUUID();
		TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		// personRef left null (ad-hoc recipient) so authorization's automatic e-mail
		// step (AC5) has no address to resolve and is skipped - this test is about
		// AC3's contingency switch, not the e-mail delivery path.
		NfeRecipient recipient = NfeRecipient.of(null, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste",
				"123456789", "RJ");
		TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		NfeDocument draft = NfeDocument.draft(documentId, companyId, null, NaturezaOperacao.VENDA, new Cfop("5102"),
				recipient, List.of(item), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, totals,
				Instant.now());
		return draft.queue("001", 1L, "3".repeat(44));
	}

	private byte[] loadCertificateFixture() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("/certificates/test-a1.pfx")) {
			return in.readAllBytes();
		}
	}
}
