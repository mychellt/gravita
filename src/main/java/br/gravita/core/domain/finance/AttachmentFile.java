package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;

/** A document as uploaded, before it is stored: its name, media type and bytes. */
public record AttachmentFile(String fileName, String contentType, byte[] content) {

	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

	public AttachmentFile {
		if (fileName == null || fileName.isBlank()) {
			throw new BusinessRuleException("file name is required");
		}
		if (content == null || content.length == 0) {
			throw new BusinessRuleException("file must not be empty");
		}
		contentType = contentType == null || contentType.isBlank() ? DEFAULT_CONTENT_TYPE : contentType;
	}
}
