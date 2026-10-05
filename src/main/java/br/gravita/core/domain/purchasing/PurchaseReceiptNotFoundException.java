package br.gravita.core.domain.purchasing;

import java.util.UUID;

public class PurchaseReceiptNotFoundException extends RuntimeException {

	public PurchaseReceiptNotFoundException(final UUID purchaseReceiptId) {
		super("Purchase receipt not found: " + purchaseReceiptId);
	}
}
