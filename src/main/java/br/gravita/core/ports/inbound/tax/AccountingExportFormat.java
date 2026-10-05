package br.gravita.core.ports.inbound.tax;

/** The file formats an accounting export is written in. */
public enum AccountingExportFormat {
	CSV("csv", "text/csv"),
	TXT("txt", "text/plain");

	private final String extension;
	private final String contentType;

	AccountingExportFormat(final String extension, final String contentType) {
		this.extension = extension;
		this.contentType = contentType;
	}

	public String extension() {
		return extension;
	}

	public String contentType() {
		return contentType;
	}
}
