package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.ApproveSalesOrderCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApproveSalesOrderRequest(@NotNull UUID approvedBy) {

	public ApproveSalesOrderCommand toCommand(final UUID orderId) {
		return new ApproveSalesOrderCommand(orderId, approvedBy);
	}
}
