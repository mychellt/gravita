package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.CashClosingReport;

public interface PrintNonFiscalReceiptPort {

	void print(CashClosingReport report);
}
