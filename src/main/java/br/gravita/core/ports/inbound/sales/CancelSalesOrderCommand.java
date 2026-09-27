package br.gravita.core.ports.inbound.sales;

import java.util.Objects;
import java.util.UUID;

public record CancelSalesOrderCommand(UUID orderId, String reason) {

	public CancelSalesOrderCommand {
		Objects.requireNonNull(orderId, "orderId is required");
	}
}
