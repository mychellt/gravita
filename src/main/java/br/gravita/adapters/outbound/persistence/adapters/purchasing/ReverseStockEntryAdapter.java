package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort;
import org.springframework.stereotype.Component;

/**
 * Inventory (M5) does not exist yet, so there is no stock ledger to write to.
 * This stub keeps {@code ReturnToSupplierService} runnable end-to-end until M5
 * ships its own {@link ReverseStockEntryPort} adapter backed by real
 * stock-balance tables.
 */
@Component
class ReverseStockEntryAdapter implements ReverseStockEntryPort {

	@Override
	public void reverseEntry(ReverseStockEntryCommand command) {
		// No-op until M5 (inventory) lands its stock-exit use case (GRA-11).
	}
}
