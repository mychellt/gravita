package br.gravita.core.ports.inbound.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ReturnSalesOrderCommand(UUID orderId, List<Item> items) {

	public ReturnSalesOrderCommand {
		Objects.requireNonNull(orderId, "orderId is required");
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("items must not be empty");
		}
		items = List.copyOf(items);
	}

	public record Item(UUID productOrServiceId, BigDecimal quantity) {
		public Item {
			Objects.requireNonNull(productOrServiceId, "productOrServiceId is required");
			Objects.requireNonNull(quantity, "quantity is required");
		}
	}
}
