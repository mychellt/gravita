package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RegisterStockExitCommand(UUID productId, UUID warehouseId, BigDecimal quantity, LotRef lot,
		List<String> serials, UUID reservationId, boolean allowNegativeStock, String originReference, UUID user) {

	public RegisterStockExitCommand {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(originReference, "originReference is required");
		Objects.requireNonNull(user, "user is required");
		serials = serials == null ? List.of() : List.copyOf(serials);
	}

	public record LotRef(String code) {
		public LotRef {
			Objects.requireNonNull(code, "lot code is required");
		}
	}
}
