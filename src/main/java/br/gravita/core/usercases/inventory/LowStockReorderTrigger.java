package br.gravita.core.usercases.inventory;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.CreatePurchaseRequestPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LowStockReorderTrigger {

	private final SuggestReorderUseCase suggestReorderUseCase;
	private final CreatePurchaseRequestPort createPurchaseRequestPort;

	public LowStockReorderTrigger(final SuggestReorderUseCase suggestReorderUseCase,
			final CreatePurchaseRequestPort createPurchaseRequestPort) {
		this.suggestReorderUseCase = suggestReorderUseCase;
		this.createPurchaseRequestPort = createPurchaseRequestPort;
	}

	public void evaluate(final UUID warehouseId) {
		for (final ReorderSuggestion suggestion : suggestReorderUseCase.execute(new SuggestReorderQuery(warehouseId))) {
			if (suggestion.suggestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			createPurchaseRequestPort.createIfNotAlreadyOpen(
					new CreatePurchaseRequestPort.ReorderCommand(suggestion.productId(),
							suggestion.suggestedQuantity()));
		}
	}
}
