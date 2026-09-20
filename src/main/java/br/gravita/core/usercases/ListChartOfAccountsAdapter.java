package br.gravita.core.usercases;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.business.ListChartOfAccountsPort;
import br.gravita.core.ports.outbound.persistence.ChartOfAccountsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListChartOfAccountsAdapter implements ListChartOfAccountsPort {

	private final ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort;

	public ListChartOfAccountsAdapter(ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort) {
		this.chartOfAccountsRepositoryPort = chartOfAccountsRepositoryPort;
	}

	@Override
	public List<ChartOfAccountsDomain> execute(Context context) {
		return chartOfAccountsRepositoryPort.findAll();
	}
}
