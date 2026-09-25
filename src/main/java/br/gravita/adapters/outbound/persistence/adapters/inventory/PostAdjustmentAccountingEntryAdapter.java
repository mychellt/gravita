package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.ports.outbound.persistence.inventory.PostAdjustmentAccountingEntryPort;
import org.springframework.stereotype.Component;

/**
 * Finance (M8) does not exist yet, so there is no general ledger to post to.
 * This stub keeps {@code AdjustInventoryService} runnable end-to-end until M8
 * ships its own {@link PostAdjustmentAccountingEntryPort} adapter backed by a
 * real accounting-entry ledger.
 */
@Component
class PostAdjustmentAccountingEntryAdapter implements PostAdjustmentAccountingEntryPort {

	@Override
	public void postAdjustmentEntry(PostAdjustmentAccountingEntryCommand command) {
		// No-op until M8 (finance) lands its accounting-entry use case.
	}
}
