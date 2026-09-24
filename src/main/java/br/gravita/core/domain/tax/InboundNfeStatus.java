package br.gravita.core.domain.tax;

/**
 * UC-M2-10 confirms conference in a single step (no intermediate
 * "conference completed" state, unlike {@code PurchaseReceiptStatus}) - an
 * {@link InboundNfe} is either awaiting conference or confirmed into
 * stock/payables.
 */
public enum InboundNfeStatus {
	PENDING_CONFERENCE,
	CONFIRMED
}
