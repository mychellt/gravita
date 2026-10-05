package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.ports.inbound.finance.CreateManualPayableCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateManualPayableRequest(
		UUID supplierId,
		@NotNull @Positive BigDecimal amount,
		@NotNull LocalDate dueDate,
		@Valid List<CostCenterShareRequest> costCenterSplit) {

	public record CostCenterShareRequest(@NotNull UUID costCenterId, @NotNull BigDecimal percent) {
	}

	public CreateManualPayableCommand toCommand() {
		final List<CostCenterShare> split = costCenterSplit == null ? null
				: costCenterSplit.stream().map(share -> new CostCenterShare(share.costCenterId(), share.percent()))
						.toList();
		return new CreateManualPayableCommand(supplierId, amount, dueDate, split);
	}
}
