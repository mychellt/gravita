package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.PosSessionId;
import java.util.UUID;

public record OpenPosSessionResponse(UUID id) {

	public static OpenPosSessionResponse from(PosSessionId id) {
		return new OpenPosSessionResponse(id.value());
	}
}
