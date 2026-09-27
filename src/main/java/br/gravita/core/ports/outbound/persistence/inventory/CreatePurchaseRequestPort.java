package br.gravita.core.ports.outbound.persistence.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface CreatePurchaseRequestPort {

	void createIfNotAlreadyOpen(ReorderCommand command);

	record ReorderCommand(UUID productId, BigDecimal quantity) {

		public ReorderCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
		}
	}
}
