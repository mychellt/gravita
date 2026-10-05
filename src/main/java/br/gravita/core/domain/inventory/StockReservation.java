package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class StockReservation {

	private final StockReservationId id;
	private final UUID orderRef;
	private final UUID productId;
	private final UUID warehouseId;
	private final BigDecimal quantity;
	private final StockReservationStatus status;

	public StockReservation(final StockReservationId id, final UUID orderRef, final UUID productId, final UUID warehouseId,
			final BigDecimal quantity, final StockReservationStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.orderRef = Objects.requireNonNull(orderRef, "orderRef is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.quantity = Objects.requireNonNull(quantity, "quantity is required");
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static StockReservation of(final StockReservationId id, final UUID orderRef, final UUID productId, final UUID warehouseId,
			final BigDecimal quantity, final StockReservationStatus status) {
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, status);
	}

	public static StockReservation create(final StockReservationId id, final UUID orderRef, final UUID productId, final UUID warehouseId,
			final BigDecimal quantity) {
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, StockReservationStatus.ACTIVE);
	}

	public StockReservation consume() {
		if (status != StockReservationStatus.ACTIVE) {
			throw new BusinessRuleException("Reservation " + id.value() + " is not active: " + status);
		}
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, StockReservationStatus.CONSUMED);
	}

	public StockReservation release() {
		if (status != StockReservationStatus.ACTIVE) {
			throw new BusinessRuleException("Reservation " + id.value() + " is not active: " + status);
		}
		return new StockReservation(id, orderRef, productId, warehouseId, quantity, StockReservationStatus.RELEASED);
	}
}
