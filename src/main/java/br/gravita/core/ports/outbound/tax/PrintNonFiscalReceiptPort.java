package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.CashClosingReport;

/**
 * Prints (or saves as PDF) the Z report at cash-closing time (UC-M3-06, AC3).
 * A basic text/PDF output is sufficient for now; full non-fiscal printer
 * hardware integration can follow later.
 */
public interface PrintNonFiscalReceiptPort {

	void print(CashClosingReport report);
}
