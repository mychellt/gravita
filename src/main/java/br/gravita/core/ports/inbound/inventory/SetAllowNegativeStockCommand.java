package br.gravita.core.ports.inbound.inventory;

import java.util.Objects;
import java.util.UUID;

public record SetAllowNegativeStockCommand(UUID productId, boolean allowNegativeStock) {

	public SetAllowNegativeStockCommand {
		Objects.requireNonNull(productId, "productId is required");
	}
}
