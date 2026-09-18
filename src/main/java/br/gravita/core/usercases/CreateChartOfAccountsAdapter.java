package br.gravita.core.usercases;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.CreateChartOfAccountsPort;
import br.gravita.core.ports.persistence.ChartOfAccountsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateChartOfAccountsAdapter implements CreateChartOfAccountsPort {

	private final ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort;

	public CreateChartOfAccountsAdapter(ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort) {
		this.chartOfAccountsRepositoryPort = chartOfAccountsRepositoryPort;
	}

	@Override
	public ChartOfAccountsDomain execute(Context context) {
		ChartOfAccountsDomain account = context.getData(ChartOfAccountsDomain.class);
		if (account.getParentId() != null && chartOfAccountsRepositoryPort.get(account.getParentId()).isEmpty()) {
			throw new BusinessRuleException("Parent account does not exist: " + account.getParentId());
		}
		if (account.getId() == null) {
			account.setId(UUID.randomUUID());
		}
		return chartOfAccountsRepositoryPort.save(account);
	}
}
