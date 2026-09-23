package br.gravita.core.domain.purchasing;

import java.util.Objects;
import java.util.UUID;

public record PurchaseReturnId(UUID value) {

	public PurchaseReturnId {
		Objects.requireNonNull(value, "PurchaseReturnId value is required");
	}

	public static PurchaseReturnId of(UUID value) {
		return new PurchaseReturnId(value);
	}
}
