package br.gravita.core.ports.inbound.reporting;

import java.util.Objects;

/** A rendered report: its bytes, the name to save it under and its media type. */
public record ExportedFile(byte[] content, String filename, String contentType) {

	public ExportedFile {
		Objects.requireNonNull(content, "content is required");
		Objects.requireNonNull(filename, "filename is required");
		Objects.requireNonNull(contentType, "contentType is required");
	}
}
