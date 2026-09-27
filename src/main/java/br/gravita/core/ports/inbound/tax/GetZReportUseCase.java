package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.CashClosingReport;
import java.util.UUID;

public interface GetZReportUseCase {
	CashClosingReport execute(UUID sessionId);
}
