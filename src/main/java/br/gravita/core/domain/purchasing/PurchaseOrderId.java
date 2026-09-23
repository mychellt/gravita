package br.gravita.core.domain.purchasing;

import java.util.Objects;
import java.util.UUID;

public record PurchaseOrderId(UUID value) {

	public PurchaseOrderId {
		Objects.requireNonNull(value, "PurchaseOrderId value is required");
	}

	public static PurchaseOrderId of(UUID value) {
		return new PurchaseOrderId(value);
	}
}
