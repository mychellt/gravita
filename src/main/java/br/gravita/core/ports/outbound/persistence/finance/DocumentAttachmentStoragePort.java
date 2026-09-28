package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.AttachmentFile;
import br.gravita.core.domain.finance.PayableAttachment;
import br.gravita.core.domain.finance.PayableId;

public interface DocumentAttachmentStoragePort {

	/** Stores the document uploaded for {@code payableId} and returns the reference to link to the payable. */
	PayableAttachment store(PayableId payableId, AttachmentFile file);
}
