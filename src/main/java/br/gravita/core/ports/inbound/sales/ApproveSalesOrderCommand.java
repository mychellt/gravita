package br.gravita.core.ports.inbound.sales;

import java.util.Objects;
import java.util.UUID;

public record ApproveSalesOrderCommand(UUID orderId, UUID approvedBy) {

	public ApproveSalesOrderCommand {
		Objects.requireNonNull(orderId, "orderId is required");
		Objects.requireNonNull(approvedBy, "approvedBy is required");
	}
}
