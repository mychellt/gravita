package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record CommissionId(UUID value) {

	public CommissionId {
		Objects.requireNonNull(value, "CommissionId value is required");
	}

	public static CommissionId of(final UUID value) {
		return new CommissionId(value);
	}
}
