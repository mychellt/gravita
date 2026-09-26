package br.gravita.core.usercases.inventory;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.CreatePurchaseRequestPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Automatic-trigger path required by UC-M5-11 (GRA-88): called after a
 * stock-affecting operation that can lower {@code available}
 * ({@link RegisterStockExitService}, {@link ReserveStockService}), it sweeps
 * the affected warehouse for reorder suggestions and opens a purchase request
 * for each one (AC1).
 */
@Component
public class LowStockReorderTrigger {

	private final SuggestReorderUseCase suggestReorderUseCase;
	private final CreatePurchaseRequestPort createPurchaseRequestPort;

	public LowStockReorderTrigger(SuggestReorderUseCase suggestReorderUseCase,
			CreatePurchaseRequestPort createPurchaseRequestPort) {
		this.suggestReorderUseCase = suggestReorderUseCase;
		this.createPurchaseRequestPort = createPurchaseRequestPort;
	}

	public void evaluate(UUID warehouseId) {
		for (ReorderSuggestion suggestion : suggestReorderUseCase.execute(new SuggestReorderQuery(warehouseId))) {
			if (suggestion.suggestedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			createPurchaseRequestPort.createIfNotAlreadyOpen(
					new CreatePurchaseRequestPort.ReorderCommand(suggestion.productId(),
							suggestion.suggestedQuantity()));
		}
	}
}
