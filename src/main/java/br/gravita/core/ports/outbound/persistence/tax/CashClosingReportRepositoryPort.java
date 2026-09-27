package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.PosSessionId;
import java.util.Optional;

public interface CashClosingReportRepositoryPort {

	CashClosingReport save(CashClosingReport report);

	Optional<CashClosingReport> findBySessionId(PosSessionId sessionId);
}
