package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseStandard;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.ports.inbound.tax.CancelNfseCommand;
import br.gravita.core.ports.inbound.tax.CancelNfseUseCase;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.tax.IssueNfsePort;
import br.gravita.core.ports.outbound.tax.NfseCancellationRequest;
import br.gravita.core.ports.outbound.tax.NfseCancellationResult;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-M4-05. Cancels an {@code AUTHORIZED} NFSe through the {@link IssueNfsePort} adapter of its municipality's
 * registered standard - the same one that issues it - with a mandatory justification, then moves the document to
 * {@code CANCELLED}. The record is only ever updated, never deleted.
 *
 * <p>The attempt is one transaction holding the document's row lock, so two concurrent cancellations are serialized
 * (the second finds it {@code CANCELLED} and is refused). The document is only changed once the municipality has
 * confirmed: a refusal is reported as a {@link BusinessRuleException} carrying its reason, and a municipality that does
 * not answer propagates, in both cases leaving the document {@code AUTHORIZED}.
 */
@UseCase
public class CancelNfseService implements CancelNfseUseCase {

	private final NfseRepositoryPort nfseRepositoryPort;
	private final MunicipalityIntegrationRepositoryPort municipalityIntegrationRepositoryPort;
	private final Map<NfseStandard, IssueNfsePort> issuersByStandard;

	@Autowired
	public CancelNfseService(NfseRepositoryPort nfseRepositoryPort,
			MunicipalityIntegrationRepositoryPort municipalityIntegrationRepositoryPort,
			ObjectProvider<IssueNfsePort> issuers) {
		// ObjectProvider rather than List: no adapter at all is a valid deployment (every municipality manual).
		this(nfseRepositoryPort, municipalityIntegrationRepositoryPort, issuers.orderedStream().toList());
	}

	public CancelNfseService(NfseRepositoryPort nfseRepositoryPort,
			MunicipalityIntegrationRepositoryPort municipalityIntegrationRepositoryPort,
			List<IssueNfsePort> issuers) {
		this.nfseRepositoryPort = nfseRepositoryPort;
		this.municipalityIntegrationRepositoryPort = municipalityIntegrationRepositoryPort;
		this.issuersByStandard = IssueNfsePortRegistry.byStandard(issuers);
	}

	@Override
	@Transactional
	public void execute(CancelNfseCommand command) {
		if (command.justification() == null || command.justification().isBlank()) {
			throw new BusinessRuleException("justification is required");
		}

		NfseDocument document = nfseRepositoryPort.findByIdForUpdate(command.nfseId())
				.orElseThrow(() -> new ResourceNotFoundException("NFSe not found: " + command.nfseId().value()));
		if (document.getStatus() != NfseStatus.AUTHORIZED) {
			throw new BusinessRuleException("NFSe " + document.getId().value()
					+ " cannot be cancelled (current status: " + document.getStatus() + ")");
		}

		String ibgeCode = document.getProviderMunicipalityIbgeCode();
		MunicipalityIntegration integration = municipalityIntegrationRepositoryPort.findByIbgeCode(ibgeCode)
				.orElseThrow(() -> new BusinessRuleException(
						"No NFSe integration is registered for municipality " + ibgeCode));
		if (!integration.isHomologated()) {
			throw new BusinessRuleException("Municipality " + ibgeCode
					+ " is not homologated for webservice transmission; the cancellation must be requested manually");
		}
		IssueNfsePort issuer = issuersByStandard.get(integration.getStandard());
		if (issuer == null) {
			throw new BusinessRuleException("No NFSe adapter is available for standard " + integration.getStandard()
					+ " (municipality " + ibgeCode + ")");
		}

		NfseCancellationResult result = issuer
				.cancel(new NfseCancellationRequest(document, integration, command.justification()));
		if (!result.isConfirmed()) {
			throw new BusinessRuleException("The municipality refused to cancel NFSe " + document.getId().value()
					+ ": " + result.rejectionReason());
		}

		nfseRepositoryPort.save(document.cancel(command.justification(), result.cancelledAt()));
	}
}
