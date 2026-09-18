package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.ports.business.ListCostCentersPort;
import br.gravita.core.ports.persistence.CostCenterRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCostCentersAdapter implements ListCostCentersPort {

	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public ListCostCentersAdapter(CostCenterRepositoryPort costCenterRepositoryPort) {
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	public List<CostCenterDomain> execute(Context context) {
		return costCenterRepositoryPort.findAll();
	}
}
