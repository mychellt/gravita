package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DigitalCertificate;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.ports.outbound.persistence.CertificateStoragePort;
import br.gravita.core.ports.outbound.tax.SefazCancellationRequest;
import br.gravita.core.ports.outbound.tax.SefazCorrectionRequest;
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
	private final String contingencyBaseUrl;
	private final Duration timeout;

	@Autowired
	public SefazUfSubmissionAdapter(final CertificateStoragePort certificateStoragePort,
			@Value("${gravita.sefaz.base-url.homologation:https://homologacao.nfce.sefaz.example}") final String homologationBaseUrl,
			@Value("${gravita.sefaz.base-url.production:https://nfce.sefaz.example}") final String productionBaseUrl,
			@Value("${gravita.sefaz.base-url.contingency:https://svc.nfe.sefaz.example}") final String contingencyBaseUrl,
			@Value("${gravita.sefaz.timeout-ms:2500}") final long timeoutMs) {
		this(certificateStoragePort, new SefazHttpClientFactory(), homologationBaseUrl, productionBaseUrl,
				contingencyBaseUrl, timeoutMs);
	}

	SefazUfSubmissionAdapter(final CertificateStoragePort certificateStoragePort, final SefazHttpClientFactory httpClientFactory,
			final String homologationBaseUrl, final String productionBaseUrl, final String contingencyBaseUrl, final long timeoutMs) {
		this.certificateStoragePort = certificateStoragePort;
		this.httpClientFactory = httpClientFactory;
		this.homologationBaseUrl = homologationBaseUrl;
		this.productionBaseUrl = productionBaseUrl;
		this.contingencyBaseUrl = contingencyBaseUrl;
		this.timeout = Duration.ofMillis(timeoutMs);
	}

	@Override
	public SefazSubmissionResult submit(final SefazSubmissionRequest request) {
		// UC-M2-03 (AC3): a request flagged as contingency is routed to the
		// SVC-AN/SVC-RS endpoint instead of the issuer's SEFAZ-UF, modelled here
		// as a single configured fallback endpoint rather than a two-tier
		// AN/RS split - the module has no per-UF SVC routing table to pick
		// between them yet.
		final RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment(),
				request.contingency());

		final SefazAuthorizationRequestPayload payload = new SefazAuthorizationRequestPayload(request.accessKey(),
				request.saleTotal(), request.taxResult().totals().grandTotal());

		return post(client, "/nfce/autorizacao", payload, "no authorization protocol");
	}

	@Override
	public SefazSubmissionResult cancel(final SefazCancellationRequest request) {
		final RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment(), false);

		final SefazCancellationRequestPayload payload = new SefazCancellationRequestPayload(request.accessKey(),
				request.protocol(), request.reason());

		return post(client, "/nfce/cancelamento", payload, "no cancellation protocol");
	}

	@Override
	public SefazSubmissionResult voidNumberRange(final SefazVoidNumberRangeRequest request) {
		final RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment(), false);

		final SefazVoidNumberRangeRequestPayload payload = new SefazVoidNumberRangeRequestPayload(request.series(),
				request.startNumber(), request.endNumber(), request.justification());

		return post(client, "/nfe/inutilizacao", payload, "no inutilizacao protocol");
	}

	@Override
	public SefazSubmissionResult correct(final SefazCorrectionRequest request) {
		final RestClient client = resolveAuthenticatedClient(request.companyId(), request.environment(), false);

		final SefazCorrectionRequestPayload payload = new SefazCorrectionRequestPayload(request.accessKey(),
				request.sequenceNumber(), request.text());

		return post(client, "/nfe/cartacorrecao", payload, "no correction-letter protocol");
	}

	@Override
	public SefazSubmissionResult manifest(final SefazManifestationRequest request) {
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

	private RestClient resolveAuthenticatedClient(final CompanyId companyId, final SefazEnvironment environment,
			final boolean contingency) {
		final DigitalCertificate certificate = certificateStoragePort.findByCompanyId(companyId)
				.orElseThrow(() -> new BusinessRuleException(
						"No digital certificate registered for company " + companyId.value()));
		if (certificate.isExpired(Instant.now())) {
			throw new BusinessRuleException("Digital certificate for company " + companyId.value() + " has expired");
		}

		final String baseUrl = contingency ? contingencyBaseUrl
				: environment == SefazEnvironment.PRODUCTION ? productionBaseUrl : homologationBaseUrl;
		return httpClientFactory.build(certificate.getPfxPayload(), certificate.getPassword(), baseUrl, timeout);
	}

	private SefazSubmissionResult post(final RestClient client, final String uri, final Object payload, final String missingProtocolMessage) {
		try {
			final SefazAuthorizationResponsePayload response = client.post()
					.uri(uri)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(SefazAuthorizationResponsePayload.class);
			if (response == null || response.protocol() == null || response.protocol().isBlank()) {
				throw new SefazUnavailableException("SEFAZ-UF returned " + missingProtocolMessage, null);
			}
			return new SefazSubmissionResult(response.protocol());
		} catch (final RestClientException e) {
			throw new SefazUnavailableException("SEFAZ-UF unreachable: " + e.getMessage(), e);
		}
	}
}
