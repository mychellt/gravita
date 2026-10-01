package br.gravita.core.ports.inbound.reporting;

import java.util.Arrays;
import java.util.Optional;

/** The file formats a report can be exported to. */
public enum ExportFormat {
	PDF("pdf", "application/pdf"),
	XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final String extension;
	private final String contentType;

	ExportFormat(String extension, String contentType) {
		this.extension = extension;
		this.contentType = contentType;
	}

	public String extension() {
		return extension;
	}

	public String contentType() {
		return contentType;
	}

	public static Optional<ExportFormat> fromExtension(String extension) {
		return Arrays.stream(values()).filter(format -> format.extension.equalsIgnoreCase(extension == null ? "" : extension.trim()))
				.findFirst();
	}
}
