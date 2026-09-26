package br.gravita.core.ports.outbound.inventory;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;

import java.util.List;

/**
 * Pushes {@link SuggestReorderUseCase} results to the M9 "estoque minimo"
 * dashboard widget (UC-M5-11, AC3; module spec §6.2), so that consumer
 * doesn't need to poll {@code GET /api/inventory/alerts/low-stock} directly.
 */
public interface NotifyLowStockPort {

	void notify(List<ReorderSuggestion> suggestions);
}
