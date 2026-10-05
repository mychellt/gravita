package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.CreateCostCenterPort;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateCostCenterAdapter implements CreateCostCenterPort {

	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public CreateCostCenterAdapter(final CostCenterRepositoryPort costCenterRepositoryPort) {
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	public CostCenterDomain execute(final Context context) {
		final CostCenterDomain costCenter = context.getData(CostCenterDomain.class);
		if (costCenter.getParentId() != null && costCenterRepositoryPort.get(costCenter.getParentId()).isEmpty()) {
			throw new BusinessRuleException("Parent cost center does not exist: " + costCenter.getParentId());
		}
		if (costCenter.getId() == null) {
			costCenter.setId(UUID.randomUUID());
		}
		return costCenterRepositoryPort.save(costCenter);
	}
}
