package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record SettlementId(UUID value) {

	public SettlementId {
		Objects.requireNonNull(value, "SettlementId value is required");
	}

	public static SettlementId of(final UUID value) {
		return new SettlementId(value);
	}
}
