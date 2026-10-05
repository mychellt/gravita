package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.DiscriminationTemplateId;
import br.gravita.core.ports.inbound.tax.CreateDiscriminationTemplateCommand;
import br.gravita.core.ports.inbound.tax.UpdateDiscriminationTemplateCommand;
import jakarta.validation.constraints.NotBlank;

/** Body of both {@code POST} (create) and {@code PUT} (replace) on a discrimination template. */
public record DiscriminationTemplateRequest(@NotBlank String serviceCode, @NotBlank String templateText) {

	public CreateDiscriminationTemplateCommand toCreateCommand() {
		return new CreateDiscriminationTemplateCommand(serviceCode, templateText);
	}

	public UpdateDiscriminationTemplateCommand toUpdateCommand(final DiscriminationTemplateId id) {
		return new UpdateDiscriminationTemplateCommand(id, serviceCode, templateText);
	}
}
