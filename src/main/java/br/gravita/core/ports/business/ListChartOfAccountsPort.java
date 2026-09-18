package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.ChartOfAccountsDomain;

import java.util.List;

public interface ListChartOfAccountsPort extends Command<List<ChartOfAccountsDomain>> {
}
