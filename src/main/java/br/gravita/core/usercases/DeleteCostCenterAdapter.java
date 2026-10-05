package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.DeleteCostCenterPort;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import br.gravita.core.ports.outbound.persistence.FinanceUsageQueryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeleteCostCenterAdapter implements DeleteCostCenterPort {

	private final CostCenterRepositoryPort costCenterRepositoryPort;
	private final FinanceUsageQueryPort financeUsageQueryPort;

	public DeleteCostCenterAdapter(final CostCenterRepositoryPort costCenterRepositoryPort, final FinanceUsageQueryPort financeUsageQueryPort) {
		this.costCenterRepositoryPort = costCenterRepositoryPort;
		this.financeUsageQueryPort = financeUsageQueryPort;
	}

	@Override
	public Void execute(final Context context) {
		final UUID id = context.getData(UUID.class);
		costCenterRepositoryPort.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id));
		if (costCenterRepositoryPort.existsByParentId(id)) {
			throw new BusinessRuleException("Cannot delete a cost center that has child cost centers: " + id);
		}
		if (financeUsageQueryPort.isCostCenterInUse(id)) {
			throw new BusinessRuleException("Cannot delete a cost center that is in use by finance: " + id);
		}
		costCenterRepositoryPort.deleteById(id);
		return null;
	}
}
