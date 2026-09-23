package br.gravita.core.domain.purchasing;

/**
 * Full lifecycle of a {@link PurchaseOrder} (docs/specs/m6-compras.md domain
 * model). UC-M6-04 only ever produces {@code OPEN}; the remaining values are
 * driven by later use cases in this module (approve/reject, receive, close,
 * cancel).
 */
public enum PurchaseOrderStatus {
	OPEN,
	PARTIALLY_RECEIVED,
	CLOSED,
	CANCELLED
}
