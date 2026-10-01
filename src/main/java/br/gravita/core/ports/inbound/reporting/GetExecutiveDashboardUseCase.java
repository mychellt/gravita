package br.gravita.core.ports.inbound.reporting;

public interface GetExecutiveDashboardUseCase {
	ExecutiveDashboardView execute(DashboardQuery query);
}
