package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CostCenterShare;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** {@code split} replaces the payable's current cost center split; its percentages must add up to 100. */
public record SplitPayableByCostCenterCommand(UUID payableId, List<CostCenterShare> split) {

	public SplitPayableByCostCenterCommand {
		Objects.requireNonNull(payableId, "payableId is required");
		Objects.requireNonNull(split, "split is required");
	}
}
