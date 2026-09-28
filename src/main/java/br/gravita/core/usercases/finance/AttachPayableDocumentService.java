package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentCommand;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentUseCase;
import br.gravita.core.ports.outbound.persistence.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AttachPayableDocumentService implements AttachPayableDocumentUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final DocumentAttachmentStoragePort documentAttachmentStoragePort;

	public AttachPayableDocumentService(PayableRepositoryPort payableRepositoryPort,
			DocumentAttachmentStoragePort documentAttachmentStoragePort) {
		this.payableRepositoryPort = payableRepositoryPort;
		this.documentAttachmentStoragePort = documentAttachmentStoragePort;
	}

	@Override
	@Transactional
	public Payable execute(AttachPayableDocumentCommand command) {
		Payable payable = payableRepositoryPort.findById(PayableId.of(command.payableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Payable not found: " + command.payableId()));

		var attachment = documentAttachmentStoragePort.store(payable.getId(), command.file());

		return payableRepositoryPort.save(payable.withAttachment(attachment));
	}
}
