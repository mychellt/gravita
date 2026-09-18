package br.gravita.adapters.outbound.persistence;

import br.gravita.core.ports.persistence.FinanceUsageQueryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Finance (M8) does not exist yet, so there is nothing to query against. This stub keeps the
 * delete-guard in {@code DeleteCostCenterAdapter}/{@code DeleteChartOfAccountsAdapter} correct
 * (never blocks on usage it cannot see) until M8 ships its own {@link FinanceUsageQueryPort}
 * adapter backed by real expense/entry tables.
 */
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
