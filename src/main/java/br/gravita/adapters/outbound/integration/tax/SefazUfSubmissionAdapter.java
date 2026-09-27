package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.tax.SefazCancellationRequest;
import br.gravita.core.ports.outbound.tax.SefazManifestationRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SefazVoidNumberRangeRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
		RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment());

		SefazAuthorizationRequestPayload payload = new SefazAuthorizationRequestPayload(request.accessKey(),
				request.saleTotal(), request.taxResult().totals().grandTotal());

		return post(client, "/nfce/autorizacao", payload, "no authorization protocol");
	}

	@Override
	public SefazSubmissionResult cancel(SefazCancellationRequest request) {
		RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment());

		SefazCancellationRequestPayload payload = new SefazCancellationRequestPayload(request.accessKey(),
				request.protocol(), request.reason());

		return post(client, "/nfce/cancelamento", payload, "no cancellation protocol");
	}

	@Override
	public SefazSubmissionResult voidNumberRange(SefazVoidNumberRangeRequest request) {
		RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment());

		SefazVoidNumberRangeRequestPayload payload = new SefazVoidNumberRangeRequestPayload(request.series(),
				request.startNumber(), request.endNumber(), request.justification());

		return post(client, "/nfe/inutilizacao", payload, "no inutilizacao protocol");
	}

	@Override
	public SefazSubmissionResult manifest(SefazManifestationRequest request) {
		// Manifestação do destinatário isn't tied to the document-issuing
		// company's own certificate the way submit/cancel/voidNumberRange are -
		// the use case works by access key alone and may have no local
		// company/certificate to resolve at all (UC-M2-07's AC3). Real
		// production wiring needs its own company/identity resolution design
		// (tracked as a follow-up); left unimplemented here rather than guessing
		// at one.
		throw new UnsupportedOperationException(
				"SEFAZ manifestação transmission is not wired yet - see UC-M2-07 follow-up");
	}

	private RestClient resolveAuthenticatedClient(CompanyId companyId, SefazEnvironment environment) {
		DigitalCertificate certificate = certificateStoragePort.findByCompanyId(companyId)
				.orElseThrow(() -> new BusinessRuleException(
						"No digital certificate registered for company " + companyId.value()));
		if (certificate.isExpired(Instant.now())) {
			throw new BusinessRuleException("Digital certificate for company " + companyId.value() + " has expired");
		}

		String baseUrl = environment == SefazEnvironment.PRODUCTION ? productionBaseUrl : homologationBaseUrl;
		return httpClientFactory.build(certificate.getPfxPayload(), certificate.getPassword(), baseUrl, timeout);
	}

	private SefazSubmissionResult post(RestClient client, String uri, Object payload, String missingProtocolMessage) {
		try {
			SefazAuthorizationResponsePayload response = client.post()
					.uri(uri)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(SefazAuthorizationResponsePayload.class);
			if (response == null || response.protocol() == null || response.protocol().isBlank()) {
				throw new SefazUnavailableException("SEFAZ-UF returned " + missingProtocolMessage, null);
			}
			return new SefazSubmissionResult(response.protocol());
		} catch (RestClientException e) {
			throw new SefazUnavailableException("SEFAZ-UF unreachable: " + e.getMessage(), e);
		}
	}
}
