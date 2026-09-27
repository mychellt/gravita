package br.gravita.core.ports.outbound.tax;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface NotifyStockEntryPort {
	void notifyEntry(NotifyStockEntryCommand command);

	record NotifyStockEntryCommand(UUID productId, BigDecimal quantity, BigDecimal unitCost,
			UUID sourceInboundNfeId) {
		public NotifyStockEntryCommand {
			Objects.requireNonNull(productId, "productId is required");
			Objects.requireNonNull(quantity, "quantity is required");
			Objects.requireNonNull(unitCost, "unitCost is required");
			Objects.requireNonNull(sourceInboundNfeId, "sourceInboundNfeId is required");
		}
	}
}
