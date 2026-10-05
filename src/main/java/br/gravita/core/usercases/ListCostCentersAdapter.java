package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.ports.business.ListCostCentersPort;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCostCentersAdapter implements ListCostCentersPort {

	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public ListCostCentersAdapter(final CostCenterRepositoryPort costCenterRepositoryPort) {
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	public List<CostCenterDomain> execute(final Context context) {
		return costCenterRepositoryPort.findAll();
	}
}
