package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.outbound.inventory.NotifyLowStockPort;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Component
@Slf4j
class NotifyLowStockAdapter implements NotifyLowStockPort {

	@Override
	public void notify(final List<ReorderSuggestion> suggestions) {
		if (suggestions.isEmpty()) {
			return;
		}
		log.info("Low stock alert: {} product/warehouse pair(s) at or below reorder point: {}", suggestions.size(),
				suggestions);
	}
}
