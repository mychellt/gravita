package br.gravita.core.ports.outbound.persistence.inventory;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Extension point for the purchasing context (M6), which owns
 * {@code PurchaseRequest}. Backed by an adapter that delegates to M6's real
 * {@code CreatePurchaseRequestUseCase} with {@code origin: MIN_STOCK_TRIGGER}
 * (UC-M5-11, GRA-88), skipping creation when a matching request is already
 * open (AC2).
 */
public interface CreatePurchaseRequestPort {

	void createIfNotAlreadyOpen(ReorderCommand command);

	record ReorderCommand(UUID productId, BigDecimal quantity) {

		public ReorderCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
		}
	}
}
