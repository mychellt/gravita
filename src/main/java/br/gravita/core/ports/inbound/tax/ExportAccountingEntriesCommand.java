package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.time.YearMonth;
import java.util.Objects;

/**
 * {@code period} is the month whose fiscal documents are exported. No accounting-format configuration exists yet in
 * M1/M10, so a {@code format} left out is {@link AccountingExportFormat#CSV}.
 */
public record ExportAccountingEntriesCommand(CompanyId companyId, YearMonth period, AccountingExportFormat format) {

	public ExportAccountingEntriesCommand {
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(period, "period is required");
		format = format == null ? AccountingExportFormat.CSV : format;
	}
}
