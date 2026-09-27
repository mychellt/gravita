package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.core.ports.outbound.persistence.FinanceUsageQueryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class FinanceUsageQueryAdapter implements FinanceUsageQueryPort {

	@Override
	public boolean isCostCenterInUse(UUID costCenterId) {
		return false;
	}

	@Override
	public boolean isChartOfAccountInUse(UUID chartOfAccountId) {
		return false;
	}
}
