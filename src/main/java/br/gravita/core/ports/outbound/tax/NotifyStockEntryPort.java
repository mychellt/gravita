package br.gravita.core.ports.outbound.tax;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * UC-M2-10's own outbound port into {@code inventory} - named for the calling
 * module (tax), the same convention as purchasing's own {@code RegisterStockEntryPort}.
 * Kept independent from it on purpose: M2's NFe-driven confirmation and M6's
 * PO-driven one (GRA-63, {@code ConfirmPurchaseReceiptUseCase}) are separate
 * flows that must not share a port or depend on each other.
 */
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
