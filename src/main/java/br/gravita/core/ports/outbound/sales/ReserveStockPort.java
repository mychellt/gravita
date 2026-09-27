package br.gravita.core.ports.outbound.sales;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface ReserveStockPort {

	void reserve(ReserveStockForOrderCommand command);

	record ReserveStockForOrderCommand(UUID orderId, UUID productOrServiceId, BigDecimal quantity) {

		public ReserveStockForOrderCommand {
			Objects.requireNonNull(orderId, "orderId is required");
			Objects.requireNonNull(productOrServiceId, "productOrServiceId is required");
			Objects.requireNonNull(quantity, "quantity is required");
		}
	}
}
