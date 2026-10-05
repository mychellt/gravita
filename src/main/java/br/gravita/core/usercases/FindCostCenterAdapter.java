package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.FindCostCenterPort;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FindCostCenterAdapter implements FindCostCenterPort {

	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public FindCostCenterAdapter(final CostCenterRepositoryPort costCenterRepositoryPort) {
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	public CostCenterDomain execute(final Context context) {
		final UUID id = context.getData(UUID.class);
		return costCenterRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id));
	}
}
