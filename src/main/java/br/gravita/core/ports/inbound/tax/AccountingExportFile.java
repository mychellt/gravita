package br.gravita.core.ports.inbound.tax;

/** The export of a period as bytes ready to be saved, and how many fiscal entries it carries. */
public record AccountingExportFile(String fileName, AccountingExportFormat format, byte[] content, int entryCount) {

	public String contentType() {
		return format.contentType();
	}
}
