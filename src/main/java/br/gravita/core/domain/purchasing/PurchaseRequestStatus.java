package br.gravita.core.domain.purchasing;

/**
 * Full lifecycle of a {@link PurchaseRequest} (docs/specs/m6-compras.md domain
 * model). UC-M6-01 only ever produces {@code OPEN}; the remaining values are
 * driven by later use cases in the module (send quotation, convert to order,
 * cancel).
 */
public enum PurchaseRequestStatus {
	OPEN,
	QUOTED,
	CONVERTED,
	CANCELLED
}
