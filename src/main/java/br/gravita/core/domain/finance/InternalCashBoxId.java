package br.gravita.core.domain.finance;

import java.util.Objects;
import java.util.UUID;

public record InternalCashBoxId(UUID value) {

	/** The back office has a single physical cash box; it is created by the migration under this id. */
	public static final InternalCashBoxId MAIN = new InternalCashBoxId(
			UUID.fromString("00000000-0000-0000-0000-000000000001"));

	public InternalCashBoxId {
		Objects.requireNonNull(value, "InternalCashBoxId value is required");
	}

	public static InternalCashBoxId of(UUID value) {
		return new InternalCashBoxId(value);
	}
}
