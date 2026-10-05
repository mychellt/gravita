package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.ManifestationType;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeCommand;

public record ManifestInboundNfeRequest(String accessKey, String type) {

	public ManifestInboundNfeCommand toCommand() {
		return new ManifestInboundNfeCommand(accessKey, parseType(type));
	}

	private static ManifestationType parseType(final String type) {
		try {
			return ManifestationType.valueOf(type == null ? null : type.toUpperCase());
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new BusinessRuleException("Unknown manifestation type: " + type);
		}
	}
}
