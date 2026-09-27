package br.gravita.core.domain.sales;

import java.util.Objects;
import java.util.UUID;

public record OpportunityId(UUID value) {

	public OpportunityId {
		Objects.requireNonNull(value, "OpportunityId value is required");
	}

	public static OpportunityId of(UUID value) {
		return new OpportunityId(value);
	}
}
