package br.gravita.core.ports.outbound.sales;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Reverts stock for a customer return (UC-M7-07) through M5's
 * {@code RegisterStockEntryUseCase} - the same use case
 * {@code purchasing.RegisterStockEntryAdapter} already wires purchase
 * receipts through, just with a sales-return origin reference.
 */
public interface RegisterStockEntryPort {

	void registerEntry(RegisterStockEntryCommand command);

	record RegisterStockEntryCommand(UUID productOrServiceId, BigDecimal quantity, BigDecimal unitCost,
			UUID sourceSalesReturnId) {

		public RegisterStockEntryCommand {
			Objects.requireNonNull(productOrServiceId, "productOrServiceId is required");
			Objects.requireNonNull(quantity, "quantity is required");
			Objects.requireNonNull(unitCost, "unitCost is required");
			Objects.requireNonNull(sourceSalesReturnId, "sourceSalesReturnId is required");
		}
	}
}
