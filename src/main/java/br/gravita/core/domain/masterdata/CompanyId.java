package br.gravita.core.domain.masterdata;

import java.util.Objects;
import java.util.UUID;

public record CompanyId(UUID value) {

	public CompanyId {
		Objects.requireNonNull(value, "CompanyId value is required");
	}

	public static CompanyId of(UUID value) {
		return new CompanyId(value);
	}
}
