package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code reservationId} is set when the exit fulfills an existing
 * {@code StockReservation} (UC-M5-06); the reserved quantity must match
 * {@code quantity} exactly. {@code allowNegativeStock} is the caller-supplied
 * negative-stock setting for this product/warehouse (module spec §6.2) - no
 * such per-product/warehouse configuration exists yet in the M1 product
 * model, so the caller (e.g. M2/M3's confirmed-sale flow) is expected to pass
 * it through once it does.
 */
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
