package br.gravita.adapters.inbound.controllers.sales.dtos;

import br.gravita.core.ports.inbound.sales.ReturnSalesOrderCommand;
import br.gravita.core.ports.inbound.sales.ReturnSalesOrderCommand.Item;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReturnSalesOrderRequest(@NotEmpty List<@Valid ItemRequest> items) {

	public ReturnSalesOrderCommand toCommand(UUID orderId) {
		return new ReturnSalesOrderCommand(orderId, items.stream().map(ItemRequest::toDomain).toList());
	}

	public record ItemRequest(@NotNull UUID productOrServiceId, @NotNull @Positive BigDecimal quantity) {
		Item toDomain() {
			return new Item(productOrServiceId, quantity);
		}
	}
}
