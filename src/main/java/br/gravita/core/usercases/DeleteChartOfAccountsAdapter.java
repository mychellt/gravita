package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.DeleteChartOfAccountsPort;
import br.gravita.core.ports.persistence.ChartOfAccountsRepositoryPort;
import br.gravita.core.ports.persistence.FinanceUsageQueryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeleteChartOfAccountsAdapter implements DeleteChartOfAccountsPort {

	private final ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort;
	private final FinanceUsageQueryPort financeUsageQueryPort;

	public DeleteChartOfAccountsAdapter(ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort, FinanceUsageQueryPort financeUsageQueryPort) {
		this.chartOfAccountsRepositoryPort = chartOfAccountsRepositoryPort;
		this.financeUsageQueryPort = financeUsageQueryPort;
	}

	@Override
	public Void execute(Context context) {
		UUID id = context.getData(UUID.class);
		chartOfAccountsRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Chart of accounts entry not found: " + id));
		if (chartOfAccountsRepositoryPort.existsByParentId(id)) {
			throw new BusinessRuleException("Cannot delete an account that has child accounts: " + id);
		}
		if (financeUsageQueryPort.isChartOfAccountInUse(id)) {
			throw new BusinessRuleException("Cannot delete an account that is in use by finance: " + id);
		}
		chartOfAccountsRepositoryPort.deleteById(id);
		return null;
	}
}
