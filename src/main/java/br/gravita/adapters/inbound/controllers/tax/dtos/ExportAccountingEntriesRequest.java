package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.ports.inbound.tax.AccountingExportFormat;
import br.gravita.core.ports.inbound.tax.ExportAccountingEntriesCommand;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import java.util.UUID;

/** {@code period} is a month as {@code yyyy-MM}; {@code format} is {@code CSV} or {@code TXT}, and {@code CSV} when left out. */
public record ExportAccountingEntriesRequest(@NotNull UUID companyId, @NotNull YearMonth period,
		AccountingExportFormat format) {

	public ExportAccountingEntriesCommand toCommand() {
		return new ExportAccountingEntriesCommand(CompanyId.of(companyId), period, format);
	}
}
