package br.gravita.adapters.outbound.integration.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.CertificateType;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SefazVoidRangeRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SefazUfSubmissionAdapterTest {

	private static final String FIXTURE_PATH = "/certificates/test-a1.pfx";
	private static final String CORRECT_PASSWORD = "gravita-test-pass";

	@Mock
	private CertificateStoragePort certificateStoragePort;

	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void ac1_returnsTheProtocolWhenSefazAuthorizesTheDocument() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/nfce/autorizacao", exchange -> {
			byte[] body = "{\"protocol\":\"protocol-999\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();

		String baseUrl = "http://localhost:" + server.getAddress().getPort();
		SefazUfSubmissionAdapter adapter = adapterFor(baseUrl, baseUrl);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		SefazSubmissionResult result = adapter.submit(submissionRequest());

		assertThat(result.protocol()).isEqualTo("protocol-999");
	}

	@Test
	void ac2_anUnreachableSefazEndpointIsReportedAsUnavailable() throws Exception {
		String unreachableUrl = "http://localhost:1";
		SefazUfSubmissionAdapter adapter = adapterFor(unreachableUrl, unreachableUrl);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		assertThatThrownBy(() -> adapter.submit(submissionRequest())).isInstanceOf(SefazUnavailableException.class);
	}

	@Test
	void aMissingCertificateIsABusinessRuleViolationNotAContingencyPath() {
		SefazUfSubmissionAdapter adapter = adapterFor("http://localhost:1", "http://localhost:1");
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.submit(submissionRequest())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void anExpiredCertificateIsABusinessRuleViolation() throws Exception {
		SefazUfSubmissionAdapter adapter = adapterFor("http://localhost:1", "http://localhost:1");
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(expiredCertificate()));

		assertThatThrownBy(() -> adapter.submit(submissionRequest())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void ucM206_returnsTheProtocolWhenSefazAcceptsAVoidRangeRequest() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/nfe/inutilizacao", exchange -> {
			byte[] body = "{\"protocol\":\"void-protocol-1\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();

		String baseUrl = "http://localhost:" + server.getAddress().getPort();
		SefazUfSubmissionAdapter adapter = adapterFor(baseUrl, baseUrl);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		SefazSubmissionResult result = adapter.voidRange(new SefazVoidRangeRequest(CompanyId.of(UUID.randomUUID()),
				SefazEnvironment.HOMOLOGATION, "001", 100L, 110L, "duplicate numbering skipped"));

		assertThat(result.protocol()).isEqualTo("void-protocol-1");
	}

	private SefazUfSubmissionAdapter adapterFor(String homologationBaseUrl, String productionBaseUrl) {
		return new SefazUfSubmissionAdapter(certificateStoragePort, new SefazHttpClientFactory(), homologationBaseUrl,
				productionBaseUrl, 1000L);
	}

	private SefazSubmissionRequest submissionRequest() {
		TaxCalculationResult taxResult = new TaxCalculationResult(List.of(),
				new TaxCalculationTotals(new EnumMap<>(TaxType.class), BigDecimal.ZERO));
		return new SefazSubmissionRequest(CompanyId.of(UUID.randomUUID()), SefazEnvironment.HOMOLOGATION,
				"3".repeat(44), new BigDecimal("10.00"), taxResult);
	}

	private DigitalCertificate validCertificate() throws Exception {
		return DigitalCertificate.of(UUID.randomUUID(), CompanyId.of(UUID.randomUUID()), CertificateType.A1,
				loadFixture(), CORRECT_PASSWORD, Instant.now().plusSeconds(3600), Instant.now());
	}

	private DigitalCertificate expiredCertificate() throws Exception {
		return DigitalCertificate.of(UUID.randomUUID(), CompanyId.of(UUID.randomUUID()), CertificateType.A1,
				loadFixture(), CORRECT_PASSWORD, Instant.now().minusSeconds(3600), Instant.now());
	}

	private byte[] loadFixture() throws Exception {
		try (InputStream in = getClass().getResourceAsStream(FIXTURE_PATH)) {
			return in.readAllBytes();
		}
	}
}
