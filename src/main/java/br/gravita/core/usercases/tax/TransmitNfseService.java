package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.ports.inbound.tax.NfseTransmissionResult;
import br.gravita.core.ports.inbound.tax.TransmitNfseCommand;
import br.gravita.core.ports.inbound.tax.TransmitNfseUseCase;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateGuidedManualUploadPort;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import br.gravita.core.ports.outbound.tax.NfseIssueRequest;
import br.gravita.core.ports.outbound.tax.NfseIssueResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-M4-04. Transmits a {@code DRAFT} NFSe through the {@link IssueNfsePort} adapter of its municipality's registered
 * standard, or - when the municipality is not homologated - hands back the standard-correct XML and manual-upload
 * instructions without transmitting anything.
 *
 * <p>The adapter is looked up by {@link IssueNfsePort#standard()}, so a new municipal standard is one more
 * {@code IssueNfsePort} bean and no change here. The standard is that of the <em>provider's</em> municipality, the one
 * the document was numbered under.
 *
 * <p>The whole attempt is one transaction holding the document's row lock: two concurrent transmissions of the same
 * NFSe are serialized (the second finds it {@code AUTHORIZED} and is refused), and if the attempt cannot finish -
 * including a municipality that does not answer - everything rolls back and the document is still a transmittable
 * {@code DRAFT}. {@code SENT} is therefore never observable by other transactions; it only marks the attempt in flight.
 * A rejection is a decided outcome, so it commits: the document goes back to {@code DRAFT} carrying the reason.
 */
@UseCase
public class TransmitNfseService implements TransmitNfseUseCase {

	private final NfseRepositoryPort nfseRepositoryPort;
	private final MunicipalityIntegrationRepositoryPort municipalityIntegrationRepositoryPort;
	private final GenerateGuidedManualUploadPort generateGuidedManualUploadPort;
	private final XmlObjectStoragePort xmlObjectStoragePort;
	private final Map<NfseStandard, IssueNfsePort> issuersByStandard;

	@Autowired
	public TransmitNfseService(NfseRepositoryPort nfseRepositoryPort,
			MunicipalityIntegrationRepositoryPort municipalityIntegrationRepositoryPort,
			GenerateGuidedManualUploadPort generateGuidedManualUploadPort, XmlObjectStoragePort xmlObjectStoragePort,
			ObjectProvider<IssueNfsePort> issuers) {
		// ObjectProvider rather than List: no adapter at all is a valid deployment (every municipality manual).
		this(nfseRepositoryPort, municipalityIntegrationRepositoryPort, generateGuidedManualUploadPort,
				xmlObjectStoragePort, issuers.orderedStream().toList());
	}

	public TransmitNfseService(NfseRepositoryPort nfseRepositoryPort,
			MunicipalityIntegrationRepositoryPort municipalityIntegrationRepositoryPort,
			GenerateGuidedManualUploadPort generateGuidedManualUploadPort, XmlObjectStoragePort xmlObjectStoragePort,
			List<IssueNfsePort> issuers) {
		this.nfseRepositoryPort = nfseRepositoryPort;
		this.municipalityIntegrationRepositoryPort = municipalityIntegrationRepositoryPort;
		this.generateGuidedManualUploadPort = generateGuidedManualUploadPort;
		this.xmlObjectStoragePort = xmlObjectStoragePort;
		this.issuersByStandard = IssueNfsePortRegistry.byStandard(issuers);
	}

	@Override
	@Transactional
	public NfseTransmissionResult execute(TransmitNfseCommand command) {
		NfseDocument document = nfseRepositoryPort.findByIdForUpdate(command.nfseId())
				.orElseThrow(() -> new ResourceNotFoundException("NFSe not found: " + command.nfseId().value()));
		if (document.getStatus() != NfseStatus.DRAFT) {
			// Also keeps an AUTHORIZED NFSe from being issued twice, and sends an RPS back to conversion.
			throw new BusinessRuleException("NFSe " + document.getId().value()
					+ " is not eligible for transmission (current status: " + document.getStatus() + ")");
		}

		String ibgeCode = document.getProviderMunicipalityIbgeCode();
		MunicipalityIntegration integration = municipalityIntegrationRepositoryPort.findByIbgeCode(ibgeCode)
				.orElseThrow(() -> new BusinessRuleException(
						"No NFSe integration is registered for municipality " + ibgeCode));

		if (!integration.isHomologated()) {
			// AC2: not an error and not a transmission - nothing changes on the document.
			return generateGuidedManualUploadPort.generate(document, integration);
		}

		// AC1/AC4: resolved before SENT so a missing adapter leaves the document exactly as it was.
		IssueNfsePort issuer = issuersByStandard.get(integration.getStandard());
		if (issuer == null) {
			throw new BusinessRuleException("No NFSe adapter is available for standard " + integration.getStandard()
					+ " (municipality " + ibgeCode + ")");
		}

		NfseDocument sent = document.send(Instant.now());
		nfseRepositoryPort.save(sent);

		NfseIssueResult result = issuer.issue(new NfseIssueRequest(sent, integration));
		if (!result.isAuthorized()) {
			// AC3: back to DRAFT, transmittable again.
			nfseRepositoryPort.save(sent.reject(result.rejectionReason()));
			return new NfseTransmissionResult.Rejected(result.rejectionReason());
		}

		// AC5: the XML goes to object storage; the document keeps only the reference.
		String xmlReference = xmlObjectStoragePort.store(document.getProviderCompanyId(), result.xml());
		nfseRepositoryPort.save(sent.authorize(result.protocol(), result.authorizedAt(), xmlReference));
		return new NfseTransmissionResult.Authorized(result.protocol(), result.authorizedAt());
	}
}
