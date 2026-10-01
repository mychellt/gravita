package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.DiscriminationTemplate;
import java.util.UUID;

public record DiscriminationTemplateResponse(UUID id, String serviceCode, String templateText) {

	public static DiscriminationTemplateResponse from(DiscriminationTemplate template) {
		return new DiscriminationTemplateResponse(template.getId().value(), template.getServiceCode().value(),
				template.getTemplateText());
	}
}
