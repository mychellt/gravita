package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.TransportModalityDomain;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** {@code active} is optional: a new modality is active by default and an update keeps the stored flag. */
public record TransportModalityRequest(@NotBlank String name, @NotBlank String code, Boolean active) {

	public TransportModalityDomain toDomain(final UUID id) {
		return TransportModalityDomain.builder()
				.id(id)
				.name(name)
				.code(code)
				.active(active)
				.build();
	}
}
