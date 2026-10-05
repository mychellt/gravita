package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Assessment;
import java.time.YearMonth;
import java.util.UUID;

/**
 * The assessment as structured data and the EFD file it is written into: {@code txt} is the file's bytes
 * ({@code ISO-8859-1}, as the SPED validator reads it), base64 in JSON.
 */
public record SpedContribuicoesResponse(UUID companyId, YearMonth period, String fileName, byte[] txt,
		Assessment assessment) {

	public static SpedContribuicoesResponse from(final SpedContribuicoesFile file) {
		return new SpedContribuicoesResponse(file.companyId().value(), file.period(), file.fileName(), file.txt(),
				file.assessment());
	}
}
