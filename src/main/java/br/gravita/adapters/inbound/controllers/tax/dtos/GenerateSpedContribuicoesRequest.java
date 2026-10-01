package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.GenerateSpedContribuicoesCommand;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import java.util.UUID;

/** {@code period} is a month as {@code yyyy-MM}. */
public record GenerateSpedContribuicoesRequest(@NotNull UUID companyId, @NotNull YearMonth period) {

	public GenerateSpedContribuicoesCommand toCommand() {
		return new GenerateSpedContribuicoesCommand(CompanyId.of(companyId), period);
	}
}
