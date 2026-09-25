package br.gravita.core.ports.outbound.persistence.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Extension point for the finance context (M8), which owns the general
 * ledger. Inventory has no dependency on M8, so until it exists this is
 * backed by a stub adapter (mirrors {@code GeneratePayableFromReceiptPort}'s
 * pattern); M8's own accounting-entry use case is expected to supply the
 * real adapter once it lands.
 */
public interface PostAdjustmentAccountingEntryPort {

	void postAdjustmentEntry(PostAdjustmentAccountingEntryCommand command);

	record PostAdjustmentAccountingEntryCommand(UUID productId, UUID warehouseId, BigDecimal quantityDelta,
			BigDecimal unitCost, String justification, UUID user) {

		public PostAdjustmentAccountingEntryCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(warehouseId, "warehouseId is required");
			Objects.requireNonNull(quantityDelta, "quantityDelta is required");
			Objects.requireNonNull(unitCost, "unitCost is required");
			Objects.requireNonNull(justification, "justification is required");
			Objects.requireNonNull(user, "user is required");
		}
	}
}
