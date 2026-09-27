package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.CashClosingReport;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.inbound.tax.GetZReportUseCase;
import br.gravita.core.ports.outbound.persistence.tax.CashClosingReportRepositoryPort;

import java.util.UUID;

@UseCase
public class GetZReportService implements GetZReportUseCase {

	private final CashClosingReportRepositoryPort cashClosingReportRepositoryPort;

	public GetZReportService(CashClosingReportRepositoryPort cashClosingReportRepositoryPort) {
		this.cashClosingReportRepositoryPort = cashClosingReportRepositoryPort;
	}

	@Override
	public CashClosingReport execute(UUID sessionId) {
		return cashClosingReportRepositoryPort.findBySessionId(PosSessionId.of(sessionId))
				.orElseThrow(() -> new ResourceNotFoundException("Z report not found for session: " + sessionId));
	}
}
