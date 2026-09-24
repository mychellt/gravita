package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record StockReservationResponse(UUID id, UUID orderRef, UUID productId, UUID warehouseId, BigDecimal quantity,
		StockReservationStatus status) {

	public static StockReservationResponse from(StockReservation reservation) {
		return new StockReservationResponse(reservation.getId().value(), reservation.getOrderRef(),
				reservation.getProductId(), reservation.getWarehouseId(), reservation.getQuantity(),
				reservation.getStatus());
	}
}
