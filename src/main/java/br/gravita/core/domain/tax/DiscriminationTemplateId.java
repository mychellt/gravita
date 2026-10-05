package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record DiscriminationTemplateId(UUID value) {

	public DiscriminationTemplateId {
		Objects.requireNonNull(value, "DiscriminationTemplateId value is required");
	}

	public static DiscriminationTemplateId of(final UUID value) {
		return new DiscriminationTemplateId(value);
	}
}
