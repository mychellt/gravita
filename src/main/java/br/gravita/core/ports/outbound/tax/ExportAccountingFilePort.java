package br.gravita.core.ports.outbound.tax;

import br.gravita.core.ports.inbound.tax.AccountingEntry;
import br.gravita.core.ports.inbound.tax.AccountingExportFormat;
import java.util.List;

/**
 * Writes a period's accounting entries as a CSV or TXT file (UC-M2-14). It owns the layout of the accounting system
 * the company is configured for; no such configuration exists in M1/M10 yet, so the one layout it writes is the
 * default for both formats.
 */
public interface ExportAccountingFilePort {

	/** The file's bytes, one line per entry in the order given. */
	byte[] export(List<AccountingEntry> entries, AccountingExportFormat format);
}
