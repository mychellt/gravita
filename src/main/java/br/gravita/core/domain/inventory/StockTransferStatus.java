package br.gravita.core.domain.inventory;

/**
 * Lifecycle of a {@link StockTransfer} (UC-M5-05): starts {@code PENDING}
 * once initiated and becomes {@code CONFIRMED} exactly once, at the
 * destination - confirming twice, or confirming one that was never
 * initiated, is rejected (AC3).
 */
public enum StockTransferStatus {
	PENDING,
	CONFIRMED
}
