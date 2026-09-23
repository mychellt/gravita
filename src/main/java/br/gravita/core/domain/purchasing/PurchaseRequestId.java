package br.gravita.core.domain.purchasing;

import java.util.Objects;
import java.util.UUID;

public record PurchaseRequestId(UUID value) {

	public PurchaseRequestId {
		Objects.requireNonNull(value, "PurchaseRequestId value is required");
	}

	public static PurchaseRequestId of(UUID value) {
		return new PurchaseRequestId(value);
	}
}
