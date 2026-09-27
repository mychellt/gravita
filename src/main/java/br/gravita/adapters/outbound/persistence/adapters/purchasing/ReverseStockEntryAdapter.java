package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort;
import org.springframework.stereotype.Component;

@Component
class ReverseStockEntryAdapter implements ReverseStockEntryPort {

	@Override
	public void reverseEntry(ReverseStockEntryCommand command) {
	}
}
