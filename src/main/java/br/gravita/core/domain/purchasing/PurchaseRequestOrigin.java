package br.gravita.core.domain.purchasing;

/**
 * Where a {@link PurchaseRequest} came from. {@code MIN_STOCK_TRIGGER} and
 * {@code SALES_ORDER_DEMAND} are raised by inventory's reorder suggestion and
 * an approved sales order respectively - both external callers of the same
 * {@code CreatePurchaseRequestUseCase} port (docs/specs/m6-compras/uc-01-create-purchase-request.md).
 */
public enum PurchaseRequestOrigin {
	USER,
	MIN_STOCK_TRIGGER,
	SALES_ORDER_DEMAND
}
