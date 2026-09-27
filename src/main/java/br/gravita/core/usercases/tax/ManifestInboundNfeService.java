package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.tax.InboundManifestation;
import br.gravita.core.domain.tax.InboundManifestationId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeCommand;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeUseCase;
import br.gravita.core.ports.outbound.persistence.tax.InboundManifestationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazManifestationRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-M2-07 (Manifestação do destinatário). Works by access key alone (AC3):
 * the optional {@link InboundNfeRepositoryPort} lookup only links the record
 * back to an already-imported {@code InboundNfe} when one happens to exist -
 * it's never a precondition, since manifestation can precede M2-08/09's
 * import of the same document.
 */
@UseCase
public class ManifestInboundNfeService implements ManifestInboundNfeUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final SubmitToSefazPort submitToSefazPort;
	private final InboundManifestationRepositoryPort inboundManifestationRepositoryPort;

	public ManifestInboundNfeService(InboundNfeRepositoryPort inboundNfeRepositoryPort,
			SubmitToSefazPort submitToSefazPort, InboundManifestationRepositoryPort inboundManifestationRepositoryPort) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.submitToSefazPort = submitToSefazPort;
		this.inboundManifestationRepositoryPort = inboundManifestationRepositoryPort;
	}

	@Override
	public InboundManifestation execute(ManifestInboundNfeCommand command) {
		Optional<InboundNfe> matchingInboundNfe = inboundNfeRepositoryPort.findByAccessKey(command.accessKey());

		// SEFAZ must accept the manifestation before the local record is created,
		// same ordering as UC-M2-06's void-number-range submission.
		SefazSubmissionResult result = submitToSefazPort
				.manifest(new SefazManifestationRequest(command.accessKey(), command.type()));

		InboundManifestation manifestation = InboundManifestation.of(InboundManifestationId.of(UUID.randomUUID()),
				command.accessKey(), command.type(), matchingInboundNfe.map(InboundNfe::getId).orElse(null),
				result.protocol(), Instant.now());

		return inboundManifestationRepositoryPort.save(manifestation);
	}
}
