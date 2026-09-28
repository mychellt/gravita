package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.AttachmentFile;
import java.util.Objects;
import java.util.UUID;

public record AttachPayableDocumentCommand(UUID payableId, AttachmentFile file) {

	public AttachPayableDocumentCommand {
		Objects.requireNonNull(payableId, "payableId is required");
		Objects.requireNonNull(file, "file is required");
	}
}
