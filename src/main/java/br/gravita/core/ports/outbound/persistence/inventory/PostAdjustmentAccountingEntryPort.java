package br.gravita.core.ports.outbound.persistence.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
