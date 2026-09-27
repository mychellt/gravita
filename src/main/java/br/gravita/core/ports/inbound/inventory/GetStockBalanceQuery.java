package br.gravita.core.ports.inbound.inventory;

import java.util.Objects;
import java.util.UUID;

public record GetStockBalanceQuery(UUID productId, UUID warehouseId) {

	public GetStockBalanceQuery {
		Objects.requireNonNull(productId, "productId is required");
	}
}
