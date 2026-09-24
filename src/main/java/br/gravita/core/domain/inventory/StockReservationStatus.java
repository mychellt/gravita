package br.gravita.core.domain.inventory;

/**
 * Lifecycle of a {@link StockReservation} (module spec §"Domain model"): it
 * starts {@code ACTIVE} and must end up either {@code CONSUMED} (fulfilled by
 * UC-M5-03's stock exit) or {@code RELEASED} (UC-M5-07, order cancellation) —
 * never left {@code ACTIVE} and stale indefinitely.
 */
public enum StockReservationStatus {
	ACTIVE,
	CONSUMED,
	RELEASED
}
