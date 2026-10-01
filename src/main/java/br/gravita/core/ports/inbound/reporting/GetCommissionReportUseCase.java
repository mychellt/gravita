package br.gravita.core.ports.inbound.reporting;

import java.util.List;

public interface GetCommissionReportUseCase {
	List<CommissionReportEntry> execute(CommissionReportQuery query);
}
