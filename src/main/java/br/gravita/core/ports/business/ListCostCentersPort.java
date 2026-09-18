package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.CostCenterDomain;

import java.util.List;

public interface ListCostCentersPort extends Command<List<CostCenterDomain>> {
}
