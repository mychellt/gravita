package br.gravita.core.domain.purchasing;

import java.util.Objects;
import java.util.UUID;

public record PurchaseReceiptId(UUID value) {

	public PurchaseReceiptId {
		Objects.requireNonNull(value, "PurchaseReceiptId value is required");
	}

	public static PurchaseReceiptId of(final UUID value) {
		return new PurchaseReceiptId(value);
	}
}
