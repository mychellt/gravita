package br.gravita.core.ports.outbound.persistence;

import java.util.UUID;

public interface FinanceUsageQueryPort {
	boolean isCostCenterInUse(final UUID costCenterId);
	boolean isChartOfAccountInUse(final UUID chartOfAccountId);
}
