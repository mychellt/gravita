package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.ports.outbound.persistence.inventory.PostAdjustmentAccountingEntryPort;
import org.springframework.stereotype.Component;

@Component
class PostAdjustmentAccountingEntryAdapter implements PostAdjustmentAccountingEntryPort {

	@Override
	public void postAdjustmentEntry(PostAdjustmentAccountingEntryCommand command) {
	}
}
