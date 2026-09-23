package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Extension point for the inventory context (M5), which owns stock balances.
 * Mirrors {@link RegisterStockEntryPort} in reverse: UC-M6-09 calls this per
 * returned item so the stock taken back out of the warehouse is reflected
 * once M5 exists. Until then this is backed by a stub adapter, same as
 * {@link RegisterStockEntryPort}; M5's own stock-exit use case (GRA-11) is
 * expected to supply the real adapter once it lands.
 */
public interface ReverseStockEntryPort {
	void reverseEntry(ReverseStockEntryCommand command);

	record ReverseStockEntryCommand(UUID productId, BigDecimal quantity, BigDecimal unitCost,
			UUID sourcePurchaseReturnId) {

		public ReverseStockEntryCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
			Objects.requireNonNull(unitCost, "unitCost is required");
			Objects.requireNonNull(sourcePurchaseReturnId, "sourcePurchaseReturnId is required");
		}
	}
}
