package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.CancelSalesOrderCommand;
import java.util.UUID;

public record CancelSalesOrderRequest(String reason) {

	public CancelSalesOrderCommand toCommand(UUID orderId) {
		return new CancelSalesOrderCommand(orderId, reason);
	}
}
