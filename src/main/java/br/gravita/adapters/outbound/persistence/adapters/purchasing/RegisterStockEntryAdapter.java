package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort;
import org.springframework.stereotype.Component;

/**
 * Inventory (M5) does not exist yet, so there is no stock ledger to write to.
 * This stub keeps {@code ConfirmPurchaseReceiptService} runnable end-to-end
 * until M5 ships its own {@link RegisterStockEntryPort} adapter backed by
 * real stock-balance tables.
 */
@Component
class RegisterStockEntryAdapter implements RegisterStockEntryPort {

	@Override
	public void registerEntry(RegisterStockEntryCommand command) {
		// No-op until M5 (inventory) lands its RegisterStockEntryUseCase (GRA-11).
	}
}
