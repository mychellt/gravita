package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.ports.inbound.inventory.StockMovementItemView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockMovementItemResponse(UUID id, StockMovementType type, UUID productId, String productCode,
		UUID warehouseId, BigDecimal quantity, BigDecimal unitCost, String lotCode, List<String> serialNumbers,
		String originReference, String justification, UUID user, Instant timestamp) {

	public static StockMovementItemResponse from(final StockMovementItemView view) {
		final StockMovement movement = view.movement();
		return new StockMovementItemResponse(movement.getId().value(), movement.getType(), movement.getProductId(),
				view.productCode(), movement.getWarehouseId(), movement.getQuantity(), movement.getUnitCost(),
				movement.getLotCode(), movement.getSerialNumbers(), movement.getOriginReference(),
				movement.getJustification(), movement.getUser(), movement.getTimestamp());
	}
}
