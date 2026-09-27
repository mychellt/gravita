package br.gravita.core.ports.inbound.sales;

import java.util.Objects;
import java.util.UUID;

public record InvoiceSalesOrderCommand(UUID orderId) {

	public InvoiceSalesOrderCommand {
		Objects.requireNonNull(orderId, "orderId is required");
	}
}
