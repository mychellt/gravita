package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentCommand;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentUseCase;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.Document;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class AttachPayableDocumentService implements AttachPayableDocumentUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final DocumentAttachmentStoragePort documentAttachmentStoragePort;

	// Not transactional on purpose: the upload must not hold a database transaction open. The payable
	// is only written once the file is stored, so a storage failure leaves it untouched.
	@Override
	public Payable execute(final AttachPayableDocumentCommand command) {
		final Payable payable = payableRepositoryPort.findById(PayableId.of(command.payableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Payable not found: " + command.payableId()));

		final AttachPayableDocumentCommand.File file = command.file();
		final String url = documentAttachmentStoragePort
				.store(new Document(file.fileName(), file.contentType(), file.content()));

		return payableRepositoryPort.save(payable.attach(url));
	}
}
