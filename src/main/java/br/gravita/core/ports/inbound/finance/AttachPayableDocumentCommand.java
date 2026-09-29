package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.Objects;
import java.util.UUID;

/** {@code file} is the uploaded boleto, NF or receipt; it must not be empty. */
public record AttachPayableDocumentCommand(UUID payableId, File file) {

	public AttachPayableDocumentCommand {
		Objects.requireNonNull(payableId, "payableId is required");
		Objects.requireNonNull(file, "file is required");
	}

	public record File(String fileName, String contentType, byte[] content) {

		public File {
			Objects.requireNonNull(fileName, "fileName is required");
			Objects.requireNonNull(contentType, "contentType is required");
			Objects.requireNonNull(content, "content is required");
			if (fileName.isBlank()) {
				throw new BusinessRuleException("fileName must not be blank");
			}
			if (content.length == 0) {
				throw new BusinessRuleException("file must not be empty");
			}
		}
	}
}
