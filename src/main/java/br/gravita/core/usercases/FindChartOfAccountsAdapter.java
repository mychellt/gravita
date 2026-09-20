package br.gravita.core.usercases;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.FindChartOfAccountsPort;
import br.gravita.core.ports.outbound.persistence.ChartOfAccountsRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindChartOfAccountsAdapter implements FindChartOfAccountsPort {

	private final ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort;

	public FindChartOfAccountsAdapter(ChartOfAccountsRepositoryPort chartOfAccountsRepositoryPort) {
		this.chartOfAccountsRepositoryPort = chartOfAccountsRepositoryPort;
	}

	@Override
	public ChartOfAccountsDomain execute(Context context) {
		UUID id = context.getData(UUID.class);
		return chartOfAccountsRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Chart of accounts entry not found: " + id));
	}
}
