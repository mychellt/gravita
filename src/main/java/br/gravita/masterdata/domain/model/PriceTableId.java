package br.gravita.masterdata.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PriceTableId(UUID value) {

	public PriceTableId {
		Objects.requireNonNull(value, "PriceTableId value is required");
	}

	public static PriceTableId of(UUID value) {
		return new PriceTableId(value);
	}
}
