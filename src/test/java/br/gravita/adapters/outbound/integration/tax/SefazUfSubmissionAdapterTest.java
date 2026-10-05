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
import br.gravita.core.ports.outbound.tax.SefazVoidNumberRangeRequest;
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
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("Returns the protocol when SEFAZ authorizes the document")
	void ac1ReturnsTheProtocolWhenSefazAuthorizesTheDocument() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/nfce/autorizacao", exchange -> {
			final byte[] body = "{\"protocol\":\"protocol-999\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();

		final String baseUrl = "http://localhost:" + server.getAddress().getPort();
		final SefazUfSubmissionAdapter adapter = adapterFor(baseUrl, baseUrl);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		final SefazSubmissionResult result = adapter.submit(submissionRequest());

		assertThat(result.protocol()).isEqualTo("protocol-999");
	}

	@Test
	@DisplayName("Reports an unreachable SEFAZ endpoint as unavailable")
	void ac2AnUnreachableSefazEndpointIsReportedAsUnavailable() throws Exception {
		final String unreachableUrl = "http://localhost:1";
		final SefazUfSubmissionAdapter adapter = adapterFor(unreachableUrl, unreachableUrl);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		assertThatThrownBy(() -> adapter.submit(submissionRequest())).isInstanceOf(SefazUnavailableException.class);
	}

	@Test
	@DisplayName("Routes to the contingency endpoint when the request is flagged for contingency")
	void ac3RoutesToTheContingencyEndpointWhenTheRequestIsFlaggedForContingency() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/nfce/autorizacao", exchange -> {
			final byte[] body = "{\"protocol\":\"svc-protocol-1\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();
		final String contingencyUrl = "http://localhost:" + server.getAddress().getPort();
		// The UF endpoints are unreachable - only the contingency (SVC-AN/SVC-RS)
		// endpoint can possibly answer, proving the request was routed there.
		final SefazUfSubmissionAdapter adapter = new SefazUfSubmissionAdapter(certificateStoragePort,
				new SefazHttpClientFactory(), "http://localhost:1", "http://localhost:1", contingencyUrl, 1000L);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		final SefazSubmissionResult result = adapter.submit(submissionRequest(true));

		assertThat(result.protocol()).isEqualTo("svc-protocol-1");
	}

	@Test
	@DisplayName("Treats a missing certificate as a business rule violation, not a contingency path")
	void missingCertificateIsABusinessRuleViolationNotAContingencyPath() {
		final SefazUfSubmissionAdapter adapter = adapterFor("http://localhost:1", "http://localhost:1");
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.submit(submissionRequest())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Treats an expired certificate as a business rule violation")
	void anExpiredCertificateIsABusinessRuleViolation() throws Exception {
		final SefazUfSubmissionAdapter adapter = adapterFor("http://localhost:1", "http://localhost:1");
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(expiredCertificate()));

		assertThatThrownBy(() -> adapter.submit(submissionRequest())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Returns the protocol when SEFAZ accepts a number range voiding request")
	void ucM206ReturnsTheProtocolWhenSefazAcceptsAVoidRangeRequest() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/nfe/inutilizacao", exchange -> {
			final byte[] body = "{\"protocol\":\"void-protocol-1\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.start();

		final String baseUrl = "http://localhost:" + server.getAddress().getPort();
		final SefazUfSubmissionAdapter adapter = adapterFor(baseUrl, baseUrl);
		when(certificateStoragePort.findByCompanyId(any())).thenReturn(Optional.of(validCertificate()));

		final SefazSubmissionResult result = adapter.voidNumberRange(new SefazVoidNumberRangeRequest(
				CompanyId.of(UUID.randomUUID()), SefazEnvironment.HOMOLOGATION, "001", 100L, 110L,
				"duplicate numbering skipped"));

		assertThat(result.protocol()).isEqualTo("void-protocol-1");
	}

	private SefazUfSubmissionAdapter adapterFor(final String homologationBaseUrl, final String productionBaseUrl) {
		return new SefazUfSubmissionAdapter(certificateStoragePort, new SefazHttpClientFactory(), homologationBaseUrl,
				productionBaseUrl, "http://localhost:1", 1000L);
	}

	private SefazSubmissionRequest submissionRequest() {
		return submissionRequest(false);
	}

	private SefazSubmissionRequest submissionRequest(final boolean contingency) {
		final TaxCalculationResult taxResult = new TaxCalculationResult(List.of(),
				new TaxCalculationTotals(new EnumMap<>(TaxType.class), BigDecimal.ZERO));
		return new SefazSubmissionRequest(CompanyId.of(UUID.randomUUID()), SefazEnvironment.HOMOLOGATION,
				"3".repeat(44), new BigDecimal("10.00"), taxResult, contingency);
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
