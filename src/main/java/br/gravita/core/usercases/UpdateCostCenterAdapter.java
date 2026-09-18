package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.UpdateCostCenterPort;
import br.gravita.core.ports.persistence.CostCenterRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UpdateCostCenterAdapter implements UpdateCostCenterPort {

	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public UpdateCostCenterAdapter(CostCenterRepositoryPort costCenterRepositoryPort) {
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	public CostCenterDomain execute(Context context) {
		CostCenterDomain costCenter = context.getData(CostCenterDomain.class);
		costCenterRepositoryPort.get(costCenter.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + costCenter.getId()));
		if (costCenter.getParentId() != null) {
			if (costCenter.getParentId().equals(costCenter.getId())) {
				throw new BusinessRuleException("A cost center cannot be its own parent");
			}
			if (costCenterRepositoryPort.get(costCenter.getParentId()).isEmpty()) {
				throw new BusinessRuleException("Parent cost center does not exist: " + costCenter.getParentId());
			}
		}
		return costCenterRepositoryPort.save(costCenter);
	}
}
