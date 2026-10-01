package br.gravita.core.ports.inbound.tax;

import java.util.Objects;

public record CreateDiscriminationTemplateCommand(String serviceCode, String templateText) {

	public CreateDiscriminationTemplateCommand {
		Objects.requireNonNull(serviceCode, "serviceCode is required");
		Objects.requireNonNull(templateText, "templateText is required");
	}
}
