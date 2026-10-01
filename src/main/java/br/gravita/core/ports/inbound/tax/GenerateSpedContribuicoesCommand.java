package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.time.YearMonth;
import java.util.Objects;

/** {@code period} is the month whose PIS/COFINS is assessed. */
public record GenerateSpedContribuicoesCommand(CompanyId companyId, YearMonth period) {

	public GenerateSpedContribuicoesCommand {
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(period, "period is required");
	}
}
