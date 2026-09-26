package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real-time SEFAZ-UF client (UC-M3-04): resolves the issuing company's A1
 * certificate (M1-UC02) for mutual-TLS client authentication and posts to
 * the environment's endpoint (M1-UC03's environment switch). Short timeouts
 * (doc §4.2's &lt;3s budget) mean an unreachable/slow SEFAZ surfaces as
 * {@link SefazUnavailableException} quickly rather than hanging the cashier.
 */
@Component
public class SefazUfSubmissionAdapter implements SubmitToSefazPort {

	private final CertificateStoragePort certificateStoragePort;
	private final SefazHttpClientFactory httpClientFactory;
	private final String homologationBaseUrl;
	private final String productionBaseUrl;
	private final Duration timeout;

	@Autowired
	public SefazUfSubmissionAdapter(CertificateStoragePort certificateStoragePort,
			@Value("${gravita.sefaz.base-url.homologation:https://homologacao.nfce.sefaz.example}") String homologationBaseUrl,
			@Value("${gravita.sefaz.base-url.production:https://nfce.sefaz.example}") String productionBaseUrl,
			@Value("${gravita.sefaz.timeout-ms:2500}") long timeoutMs) {
		this(certificateStoragePort, new SefazHttpClientFactory(), homologationBaseUrl, productionBaseUrl, timeoutMs);
	}

	SefazUfSubmissionAdapter(CertificateStoragePort certificateStoragePort, SefazHttpClientFactory httpClientFactory,
			String homologationBaseUrl, String productionBaseUrl, long timeoutMs) {
		this.certificateStoragePort = certificateStoragePort;
		this.httpClientFactory = httpClientFactory;
		this.homologationBaseUrl = homologationBaseUrl;
		this.productionBaseUrl = productionBaseUrl;
		this.timeout = Duration.ofMillis(timeoutMs);
	}

	@Override
	public SefazSubmissionResult submit(SefazSubmissionRequest request) {
		DigitalCertificate certificate = certificateStoragePort.findByCompanyId(request.companyId())
				.orElseThrow(() -> new BusinessRuleException(
						"No digital certificate registered for company " + request.companyId().value()));
		if (certificate.isExpired(Instant.now())) {
			throw new BusinessRuleException(
					"Digital certificate for company " + request.companyId().value() + " has expired");
		}

		String baseUrl = request.environment() == SefazEnvironment.PRODUCTION ? productionBaseUrl : homologationBaseUrl;
		RestClient client = httpClientFactory.build(certificate.getPfxPayload(), certificate.getPassword(), baseUrl,
				timeout);

		SefazAuthorizationRequestPayload payload = new SefazAuthorizationRequestPayload(request.accessKey(),
				request.saleTotal(), request.taxResult().totals().grandTotal());

		try {
			SefazAuthorizationResponsePayload response = client.post()
					.uri("/nfce/autorizacao")
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(SefazAuthorizationResponsePayload.class);
			if (response == null || response.protocol() == null || response.protocol().isBlank()) {
				throw new SefazUnavailableException("SEFAZ-UF returned no authorization protocol", null);
			}
			return new SefazSubmissionResult(response.protocol());
		} catch (RestClientException e) {
			throw new SefazUnavailableException("SEFAZ-UF unreachable: " + e.getMessage(), e);
		}
	}
}
