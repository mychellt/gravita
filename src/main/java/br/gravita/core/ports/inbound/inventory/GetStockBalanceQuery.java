package br.gravita.core.ports.inbound.inventory;

import java.util.Objects;
import java.util.UUID;

/**
 * {@code warehouseId} is optional: omit it to aggregate the balance across
 * every warehouse/branch the product is tracked in (UC-M5-01, AC2).
 */
public record GetStockBalanceQuery(UUID productId, UUID warehouseId) {

	public GetStockBalanceQuery {
		Objects.requireNonNull(productId, "productId is required");
	}
}
