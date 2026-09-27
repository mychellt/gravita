package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.CashClosingReport;

public interface ClosePosSessionUseCase {
	CashClosingReport execute(ClosePosSessionCommand command);
}
