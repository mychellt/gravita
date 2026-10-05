package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailCommand;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailUseCase;
import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;

/**
 * UC-M2-03 (AC5). Re-sends the already-rendered XML/DANFE from storage rather
 * than regenerating them - an authorized document's fiscal content never
 * changes, so there is nothing to re-derive.
 */
@UseCase
public class ResendNfeEmailService implements ResendNfeEmailUseCase {

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final XmlObjectStoragePort xmlObjectStoragePort;
	private final SendFiscalDocumentByEmailPort sendFiscalDocumentByEmailPort;

	public ResendNfeEmailService(final NfeRepositoryPort nfeRepositoryPort, final CustomerRepositoryPort customerRepositoryPort,
			final XmlObjectStoragePort xmlObjectStoragePort, final SendFiscalDocumentByEmailPort sendFiscalDocumentByEmailPort) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.xmlObjectStoragePort = xmlObjectStoragePort;
		this.sendFiscalDocumentByEmailPort = sendFiscalDocumentByEmailPort;
	}

	@Override
	public void execute(final ResendNfeEmailCommand command) {
		final NfeDocumentId id = NfeDocumentId.of(command.nfeDocumentId());
		final NfeDocument document = nfeRepositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("NfeDocument not found: " + command.nfeDocumentId()));

		if (document.getStatus() != NfeDocumentStatus.AUTHORIZED) {
			throw new BusinessRuleException(
					"NfeDocument " + id.value() + " is not AUTHORIZED (current status: " + document.getStatus() + ")");
		}

		final String email = NfeEmailSupport.resolveRecipientEmail(document, customerRepositoryPort)
				.orElseThrow(() -> new BusinessRuleException(
						"NfeDocument " + id.value() + " has no recipient e-mail address on file"));

		final byte[] xml = xmlObjectStoragePort.retrieve(document.getXmlStorageRef());
		final byte[] danfe = xmlObjectStoragePort.retrieve(document.getDanfeStorageRef());

		sendFiscalDocumentByEmailPort.send(NfeEmailSupport.buildEmailRequest(document, email, xml, danfe));
	}
}
