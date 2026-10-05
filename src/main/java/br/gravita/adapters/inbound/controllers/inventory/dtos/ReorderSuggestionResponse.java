package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.ports.inbound.inventory.ReorderSuggestion;

import java.math.BigDecimal;
import java.util.UUID;

public record ReorderSuggestionResponse(UUID productId, UUID warehouseId, BigDecimal available,
		BigDecimal reorderPoint, BigDecimal suggestedQuantity) {

	public static ReorderSuggestionResponse from(final ReorderSuggestion suggestion) {
		return new ReorderSuggestionResponse(suggestion.productId(), suggestion.warehouseId(), suggestion.available(),
				suggestion.reorderPoint(), suggestion.suggestedQuantity());
	}
}
