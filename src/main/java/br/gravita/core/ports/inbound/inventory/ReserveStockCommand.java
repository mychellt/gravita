package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code orderRef} is the sales order id (M7's {@code ApproveSalesOrderUseCase}
 * trigger, UC-M5-06); it is kept as an opaque reference since M7 doesn't exist
 * yet.
 */
public record ReserveStockCommand(UUID orderRef, UUID productId, UUID warehouseId, BigDecimal quantity) {

	public ReserveStockCommand {
		Objects.requireNonNull(orderRef, "orderRef is required");
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(quantity, "quantity is required");
	}
}
