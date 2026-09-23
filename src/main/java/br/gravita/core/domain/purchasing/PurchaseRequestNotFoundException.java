package br.gravita.core.domain.purchasing;

import java.util.UUID;

public class PurchaseRequestNotFoundException extends RuntimeException {

	public PurchaseRequestNotFoundException(UUID purchaseRequestId) {
		super("Purchase request not found: " + purchaseRequestId);
	}
}
