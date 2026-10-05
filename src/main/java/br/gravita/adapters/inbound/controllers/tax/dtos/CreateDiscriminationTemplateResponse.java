package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.DiscriminationTemplateId;
import java.util.UUID;

public record CreateDiscriminationTemplateResponse(UUID id) {

	public static CreateDiscriminationTemplateResponse from(final DiscriminationTemplateId id) {
		return new CreateDiscriminationTemplateResponse(id.value());
	}
}
