package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.adapters.inbound.controllers.finance.dtos.CreateManualPayableRequest.CostCenterShareRequest;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record SplitPayableRequest(@NotEmpty @Valid List<CostCenterShareRequest> costCenterSplit) {

	public SplitPayableByCostCenterCommand toCommand(final UUID payableId) {
		final List<CostCenterShare> split = costCenterSplit.stream()
				.map(share -> new CostCenterShare(share.costCenterId(), share.percent())).toList();
		return new SplitPayableByCostCenterCommand(payableId, split);
	}
}
