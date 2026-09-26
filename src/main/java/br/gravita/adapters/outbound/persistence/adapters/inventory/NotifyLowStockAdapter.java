package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.outbound.inventory.NotifyLowStockPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * M9 (dashboard) does not exist yet, so there is no widget to push low-stock
 * alerts to. This stub logs the suggestions and keeps
 * {@code SuggestReorderService} runnable end-to-end until M9 ships its own
 * {@link NotifyLowStockPort} adapter backed by a real dashboard feed - mirrors
 * {@code PostAdjustmentAccountingEntryAdapter}'s stub-pending-a-later-module
 * pattern (GRA-88).
 */
@Component
class NotifyLowStockAdapter implements NotifyLowStockPort {

	private static final Logger log = LoggerFactory.getLogger(NotifyLowStockAdapter.class);

	@Override
	public void notify(List<ReorderSuggestion> suggestions) {
		if (suggestions.isEmpty()) {
			return;
		}
		log.info("Low stock alert: {} product/warehouse pair(s) at or below reorder point: {}", suggestions.size(),
				suggestions);
	}
}
