package br.gravita.core.ports.inbound.purchasing;

import java.util.List;

/** Every purchase order, newest first. */
public interface ListPurchaseOrdersUseCase {
	List<PurchaseOrderListItem> execute();
}
