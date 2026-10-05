package br.gravita.core.ports.outbound.finance;

import java.util.Objects;

/** File storage for the documents attached to payables (boleto, NF, payment receipt). */
public interface DocumentAttachmentStoragePort {

	/**
	 * Stores {@code document} and returns the URL it can be fetched from.
	 *
	 * @throws DocumentStorageUnavailableException
	 *             if the document could not be stored
	 */
	String store(Document document);

	record Document(String fileName, String contentType, byte[] content) {
		public Document {
			Objects.requireNonNull(fileName, "fileName is required");
			Objects.requireNonNull(contentType, "contentType is required");
			Objects.requireNonNull(content, "content is required");
		}
	}

	class DocumentStorageUnavailableException extends RuntimeException {

		public DocumentStorageUnavailableException(final String message) {
			super(message);
		}

		public DocumentStorageUnavailableException(final String message, final Throwable cause) {
			super(message, cause);
		}
	}
}
