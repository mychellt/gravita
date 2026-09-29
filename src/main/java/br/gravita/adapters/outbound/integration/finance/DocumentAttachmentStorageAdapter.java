package br.gravita.adapters.outbound.integration.finance;

import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import org.springframework.stereotype.Component;

/**
 * Placeholder until a document store (e.g. S3) is chosen and configured: every
 * request fails cleanly with {@link DocumentStorageUnavailableException} rather
 * than pretending to store the file.
 */
@Component
class DocumentAttachmentStorageAdapter implements DocumentAttachmentStoragePort {

	@Override
	public String store(Document document) {
		throw new DocumentStorageUnavailableException("Document storage not configured");
	}
}
