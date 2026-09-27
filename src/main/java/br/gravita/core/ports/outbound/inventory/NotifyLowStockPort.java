package br.gravita.core.ports.outbound.inventory;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;

import java.util.List;

public interface NotifyLowStockPort {

	void notify(List<ReorderSuggestion> suggestions);
}
