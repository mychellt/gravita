package br.gravita.core.domain.purchasing;

import java.util.UUID;

public class PurchaseRequestNotFoundException extends RuntimeException {

	public PurchaseRequestNotFoundException(final UUID purchaseRequestId) {
		super("Purchase request not found: " + purchaseRequestId);
	}
}
