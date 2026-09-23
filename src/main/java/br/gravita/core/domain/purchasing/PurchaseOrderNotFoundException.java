package br.gravita.core.domain.purchasing;

import java.util.UUID;

public class PurchaseOrderNotFoundException extends RuntimeException {

	public PurchaseOrderNotFoundException(UUID purchaseOrderId) {
		super("Purchase order not found: " + purchaseOrderId);
	}
}
