package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Extension point for the inventory context (M5), which owns stock balances.
 * Purchasing has no dependency on M5, so until it exists this is backed by a
 * stub adapter (mirrors {@code FinanceUsageQueryPort}'s pattern); M5's own
 * {@code RegisterStockEntryUseCase} (GRA-11) is expected to supply the real
 * adapter once it lands.
 */
public interface RegisterStockEntryPort {
	void registerEntry(RegisterStockEntryCommand command);

	record RegisterStockEntryCommand(UUID productId, BigDecimal quantity, BigDecimal unitCost,
			UUID sourcePurchaseReceiptId) {

		public RegisterStockEntryCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
			Objects.requireNonNull(unitCost, "unitCost is required");
			Objects.requireNonNull(sourcePurchaseReceiptId, "sourcePurchaseReceiptId is required");
		}
	}
}
