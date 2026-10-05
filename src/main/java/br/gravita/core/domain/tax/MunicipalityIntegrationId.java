package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record MunicipalityIntegrationId(UUID value) {

	public MunicipalityIntegrationId {
		Objects.requireNonNull(value, "MunicipalityIntegrationId value is required");
	}

	public static MunicipalityIntegrationId of(final UUID value) {
		return new MunicipalityIntegrationId(value);
	}
}
