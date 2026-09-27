package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
