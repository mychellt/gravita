package br.gravita.adapters.inbound.controllers.masterdata.dtos;

import br.gravita.core.domain.masterdata.PriceTableId;
import java.util.UUID;

public record PriceTableResponse(UUID id) {
	public static PriceTableResponse from(final PriceTableId id) {
		return new PriceTableResponse(id.value());
	}
}
