package br.gravita.core.usercases;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.UpdateChartOfAccountsPort;
import br.gravita.core.ports.outbound.persistence.ChartOfAccountsRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UpdateChartOfAccountsAdapter implements UpdateChartOfAccountsPort {

	private final ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort;

	public UpdateChartOfAccountsAdapter(ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort) {
		this.chartOfAccountsRepositoryPort = chartOfAccountsRepositoryPort;
	}

	@Override
	public ChartOfAccountsDomain execute(Context context) {
		ChartOfAccountsDomain account = context.getData(ChartOfAccountsDomain.class);
		chartOfAccountsRepositoryPort.get(account.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Chart of accounts entry not found: " + account.getId()));
		if (account.getParentId() != null) {
			if (account.getParentId().equals(account.getId())) {
				throw new BusinessRuleException("An account cannot be its own parent");
			}
			if (chartOfAccountsRepositoryPort.get(account.getParentId()).isEmpty()) {
				throw new BusinessRuleException("Parent account does not exist: " + account.getParentId());
			}
		}
		return chartOfAccountsRepositoryPort.save(account);
	}
}
