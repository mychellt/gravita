package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.SpedFiscalFile;
import br.gravita.core.ports.inbound.tax.SpedValidationReport;

/** {@code txt} is the file, base64 of its ISO-8859-1 bytes; {@code validation} lists what the file goes out with. */
public record SpedFiscalResponse(String fileName, byte[] txt, SpedValidationReport validation) {

	public static SpedFiscalResponse from(final SpedFiscalFile file) {
		return new SpedFiscalResponse(file.fileName(), file.content(), file.report());
	}
}
