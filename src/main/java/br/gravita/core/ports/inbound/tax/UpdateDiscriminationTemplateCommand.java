package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.DiscriminationTemplateId;
import java.util.Objects;

public record UpdateDiscriminationTemplateCommand(DiscriminationTemplateId id, String serviceCode,
		String templateText) {

	public UpdateDiscriminationTemplateCommand {
		Objects.requireNonNull(id, "id is required");
		Objects.requireNonNull(serviceCode, "serviceCode is required");
		Objects.requireNonNull(templateText, "templateText is required");
	}
}
