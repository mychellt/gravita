package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
