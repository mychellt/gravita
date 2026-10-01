package br.gravita.core.ports.inbound.tax;

public interface ExportAccountingEntriesUseCase {
	AccountingExportFile execute(ExportAccountingEntriesCommand command);
}
