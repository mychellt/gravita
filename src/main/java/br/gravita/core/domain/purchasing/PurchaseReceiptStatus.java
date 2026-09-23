package br.gravita.core.domain.purchasing;

/**
 * Full lifecycle of a {@link PurchaseReceipt} (docs/specs/m6-compras.md domain
 * model). UC-M6-06 records a receipt as {@code PENDING_CONFERENCE} and moves
 * it to {@code CONFERENCE_COMPLETED} once the physical conference (and, where
 * applicable, UC-M6-07's XML reconciliation) is done; only then can UC-M6-08
 * move it to {@code CONFIRMED}.
 */
public enum PurchaseReceiptStatus {
	PENDING_CONFERENCE,
	CONFERENCE_COMPLETED,
	CONFIRMED
}
