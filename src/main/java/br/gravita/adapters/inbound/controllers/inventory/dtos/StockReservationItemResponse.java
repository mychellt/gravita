package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.ports.inbound.inventory.StockReservationItemView;
import java.math.BigDecimal;
import java.util.UUID;

public record StockReservationItemResponse(UUID id, UUID orderRef, UUID productId, String productCode,
		UUID warehouseId, BigDecimal quantity, StockReservationStatus status) {

	public static StockReservationItemResponse from(final StockReservationItemView view) {
		final StockReservation reservation = view.reservation();
		return new StockReservationItemResponse(reservation.getId().value(), reservation.getOrderRef(),
				reservation.getProductId(), view.productCode(), reservation.getWarehouseId(),
				reservation.getQuantity(), reservation.getStatus());
	}
}
