package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.InboundManifestation;
import java.time.Instant;
import java.util.UUID;

public record InboundManifestationResponse(UUID id, String accessKey, String type, String sefazProtocol,
		Instant manifestedAt) {

	public static InboundManifestationResponse from(InboundManifestation manifestation) {
		return new InboundManifestationResponse(
				manifestation.getId().value(),
				manifestation.getAccessKey(),
				manifestation.getType().name(),
				manifestation.getSefazProtocol(),
				manifestation.getManifestedAt());
	}
}
