package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Quantity held against a {@link StockBalance} for a confirmed sales order
 * (UC-M5-06). Traceable back to {@code orderRef}; later either consumed by a
 * stock exit (UC-M5-03) or released on cancellation (UC-M5-07).
 */
@Getter
public final class StockReservation {

	private final StockReservationId id;
	private final UUID orderRef;
	private final UUID productId;
	private final UUID warehouseId;
	private final BigDecimal quantity;
	private final StockReservationStatus status;

	private StockReservation(StockReservationId id, UUID orderRef, UUID productId, UUID warehouseId,
			BigDecimal quantity, StockReservationStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.orderRef = Objects.requireNonNull(orderRef, "orderRef is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.quantity = Objects.requireNonNull(quantity, "quantity is required");
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static StockReservation of(StockReservationId id, UUID orderRef, UUID productId, UUID warehouseId,
			BigDecimal quantity, StockReservationStatus status) {
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, status);
	}

	public static StockReservation create(StockReservationId id, UUID orderRef, UUID productId, UUID warehouseId,
			BigDecimal quantity) {
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, StockReservationStatus.ACTIVE);
	}

	/** Fulfills this reservation via a stock exit (UC-M5-03, AC4). */
	public StockReservation consume() {
		if (status != StockReservationStatus.ACTIVE) {
			throw new BusinessRuleException("Reservation " + id.value() + " is not active: " + status);
		}
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, StockReservationStatus.CONSUMED);
	}
}
