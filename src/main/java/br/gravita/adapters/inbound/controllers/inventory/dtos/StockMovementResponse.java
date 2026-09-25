package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockMovementResponse(UUID id, StockMovementType type, UUID productId, UUID warehouseId,
		BigDecimal quantity, BigDecimal unitCost, String lotCode, List<String> serialNumbers, String originReference,
		String justification, UUID user, Instant timestamp) {

	public static StockMovementResponse from(StockMovement movement) {
		return new StockMovementResponse(movement.getId().value(), movement.getType(), movement.getProductId(),
				movement.getWarehouseId(), movement.getQuantity(), movement.getUnitCost(), movement.getLotCode(),
				movement.getSerialNumbers(), movement.getOriginReference(), movement.getJustification(),
				movement.getUser(), movement.getTimestamp());
	}
}
